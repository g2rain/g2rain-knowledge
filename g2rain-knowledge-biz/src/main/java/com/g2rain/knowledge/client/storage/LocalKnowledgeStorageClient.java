package com.g2rain.knowledge.client.storage;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores PDF sources below a server-owned root. This initial implementation
 * intentionally accepts only PDFs because their signature can be checked
 * without claiming to safely validate every Office archive format.
 */
@Component
public class LocalKnowledgeStorageClient implements KnowledgeStorageClient {
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private final KnowledgeStorageProperties properties;

    public LocalKnowledgeStorageClient(KnowledgeStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredSource storeSource(Long organId, Long knowledgeId, Long revisionId,
                                    InputStream content, SourceMetadata metadata) {
        if (Objects.isNull(content) || Objects.isNull(metadata) || metadata.declaredSize() < 1
            || metadata.declaredSize() > properties.getMaxFileSize().toBytes()) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
        }
        String displayFileName = sanitizeDisplayName(metadata.originalFileName());
        if (!displayFileName.toLowerCase(Locale.ROOT).endsWith(".pdf")
            || !"application/pdf".equalsIgnoreCase(metadata.contentType())) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
        }

        Path root = requireRoot();
        Path sourceDirectory = root.resolve(organId.toString()).resolve(knowledgeId.toString())
            .resolve(revisionId.toString()).resolve("source").normalize();
        assertWithinRoot(root, sourceDirectory);
        ensureDirectory(sourceDirectory, root);
        Path temporary = sourceDirectory.resolve(".upload-" + UUID.randomUUID() + ".tmp");
        Path target = sourceDirectory.resolve(UUID.randomUUID() + ".pdf");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size = copyPdfAndDigest(content, temporary, digest);
            if (size < 1 || size > properties.getMaxFileSize().toBytes()) {
                Files.deleteIfExists(temporary);
                throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
            }
            moveAtomically(temporary, target);
            return new StoredSource(root.relativize(target).toString().replace('\\', '/'), displayFileName,
                "application/pdf", HexFormat.of().formatHex(digest.digest()), size);
        } catch (BusinessException exception) {
            deleteQuietly(temporary);
            throw exception;
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteQuietly(temporary);
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public void deleteUnreferenced(String relativePath) {
        if (Objects.isNull(relativePath) || relativePath.isBlank()) {
            return;
        }
        Path root = requireRoot();
        Path target = root.resolve(relativePath).normalize();
        assertWithinRoot(root, target);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public InputStream openSource(String relativePath) {
        Path root = requireRoot();
        Path target = root.resolve(relativePath).normalize();
        assertWithinRoot(root, target);
        try {
            return Files.newInputStream(target, StandardOpenOption.READ);
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public String writeParsed(Long organId, Long knowledgeId, Long revisionId, Long taskId, String content) {
        Path root = requireRoot();
        Path directory = root.resolve(organId.toString()).resolve(knowledgeId.toString()).resolve(revisionId.toString())
            .resolve("parsed").resolve(taskId.toString()).normalize();
        assertWithinRoot(root, directory);
        ensureDirectory(directory, root);
        Path target = directory.resolve("document.md");
        try {
            Files.writeString(target, content, java.nio.charset.StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public String readParsedChunk(String relativePath, String locator) {
        if (locator == null || !locator.startsWith("char:")) throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        String[] positions = locator.substring(5).split(":", -1);
        if (positions.length != 2) throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        try {
            int start = Integer.parseInt(positions[0]); int end = Integer.parseInt(positions[1]);
            Path root = requireRoot(); Path target = root.resolve(relativePath).normalize(); assertWithinRoot(root, target);
            String content = Files.readString(target, java.nio.charset.StandardCharsets.UTF_8);
            if (start < 0 || end < start || end > content.length()) throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
            return content.substring(start, end);
        } catch (IOException | NumberFormatException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    private long copyPdfAndDigest(InputStream content, Path temporary, MessageDigest digest) throws IOException {
        long total = 0;
        try (BufferedInputStream input = new BufferedInputStream(content);
             DigestInputStream digestInput = new DigestInputStream(input, digest);
             OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] prefix = digestInput.readNBytes(PDF_SIGNATURE.length);
            if (!java.util.Arrays.equals(prefix, PDF_SIGNATURE)) {
                throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
            }
            output.write(prefix);
            total += prefix.length;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = digestInput.read(buffer)) != -1) {
                total += read;
                if (total > properties.getMaxFileSize().toBytes()) {
                    throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
                }
                output.write(buffer, 0, read);
            }
        }
        return total;
    }

    private Path requireRoot() {
        if (Objects.isNull(properties.getRoot()) || properties.getRoot().isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_NOT_CONFIGURED);
        }
        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root)) {
                throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
            }
            return root.toRealPath();
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    private void ensureDirectory(Path directory, Path root) {
        try {
            Files.createDirectories(directory);
            Path current = root;
            for (Path part : root.relativize(directory)) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)) {
                    throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    private void moveAtomically(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target);
        }
    }

    private String sanitizeDisplayName(String rawName) {
        if (Objects.isNull(rawName) || rawName.isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
        }
        String name = Path.of(rawName).getFileName().toString().replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_").trim();
        if (name.isBlank() || name.length() > 512) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
        }
        return name;
    }

    private void assertWithinRoot(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The file remains unreferenced and is safe for the maintenance cleanup path.
        }
    }
}

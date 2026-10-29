package com.g2rain.knowledge.client.storage;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.knowledge.domain.ChunkContent;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores FILE sources below a server-owned root. Accepts PDF, Office (doc/docx/ppt/pptx/xls/xlsx),
 * Markdown, TXT, and HTML. Office/PDF use binary sniffing; text formats must be UTF-8.
 */
@Component
public class LocalKnowledgeStorageClient implements KnowledgeStorageClient {
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ZIP_SIGNATURE = new byte[] {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] OLE_SIGNATURE = new byte[] {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final JsonCodec JSON = JsonCodecFactory.instance();
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
        SourceFileKind kind = SourceFileKind.resolve(displayFileName, metadata.contentType());
        if (kind == null) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
        }

        Path root = requireRoot();
        Path sourceDirectory = root.resolve(organId.toString()).resolve(knowledgeId.toString())
            .resolve(revisionId.toString()).resolve("source").normalize();
        assertWithinRoot(root, sourceDirectory);
        ensureDirectory(sourceDirectory, root);
        Path temporary = sourceDirectory.resolve(".upload-" + UUID.randomUUID() + ".tmp");
        Path target = sourceDirectory.resolve(UUID.randomUUID() + kind.extension());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size = kind.binary()
                ? copyBinaryAndDigest(kind, content, temporary, digest)
                : copyTextAndDigest(content, temporary, digest);
            if (size < 1 || size > properties.getMaxFileSize().toBytes()) {
                Files.deleteIfExists(temporary);
                throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
            }
            moveAtomically(temporary, target);
            return new StoredSource(root.relativize(target).toString().replace('\\', '/'), displayFileName,
                kind.storedContentType(), HexFormat.of().formatHex(digest.digest()), size);
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
        if (!Files.isRegularFile(target)) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_FILE_MISSING);
        }
        try {
            return Files.newInputStream(target, StandardOpenOption.READ);
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_FILE_MISSING);
        }
    }

    @Override
    public String writeParsedArtifacts(Long organId, Long knowledgeId, Long revisionId, Long taskId,
                                       String documentMarkdown, List<ChunkContent> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
        Path root = requireRoot();
        Path directory = root.resolve(organId.toString()).resolve(knowledgeId.toString()).resolve(revisionId.toString())
            .resolve("parsed").resolve(taskId.toString()).normalize();
        assertWithinRoot(root, directory);
        ensureDirectory(directory, root);
        Path document = directory.resolve("document.md");
        Path chunksFile = directory.resolve("chunks.json");
        try {
            String markdown = Objects.requireNonNullElse(documentMarkdown, "");
            Files.writeString(document, markdown, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            List<StoredChunkArtifact> artifacts = chunks.stream()
                .map(c -> new StoredChunkArtifact(
                    c.chunkNo(), c.sourceLocator(), c.rawText(), c.embeddingText(),
                    c.tokenCount(), c.pageStart(), c.pageEnd(), c.titlePath(), c.contentSha256()))
                .toList();
            Files.writeString(chunksFile, JSON.obj2str(artifacts), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return root.relativize(document).toString().replace('\\', '/');
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public String readParsedChunk(String relativePath, String locator) {
        return readChunkField(relativePath, locator, true);
    }

    @Override
    public String readChunkForEmbedding(String relativePath, String locator) {
        return readChunkField(relativePath, locator, false);
    }

    private String readChunkField(String relativePath, String locator, boolean raw) {
        int chunkNo = parseChunkLocator(locator);
        StoredChunkArtifact artifact = loadChunk(relativePath, chunkNo);
        String value = raw ? artifact.rawText() : artifact.embeddingText();
        if (value == null || value.isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
        return value;
    }

    private int parseChunkLocator(String locator) {
        if (locator == null || !locator.startsWith("chunk:")) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
        try {
            int chunkNo = Integer.parseInt(locator.substring("chunk:".length()));
            if (chunkNo < 1) {
                throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
            }
            return chunkNo;
        } catch (NumberFormatException exception) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    private StoredChunkArtifact loadChunk(String relativePath, int chunkNo) {
        Path root = requireRoot();
        Path document = root.resolve(relativePath).normalize();
        assertWithinRoot(root, document);
        Path chunksFile = document.getParent() == null
            ? document.resolveSibling("chunks.json")
            : document.getParent().resolve("chunks.json");
        assertWithinRoot(root, chunksFile);
        try {
            String json = Files.readString(chunksFile, StandardCharsets.UTF_8);
            List<StoredChunkArtifact> artifacts = JSON.str2obj(json, List.class, StoredChunkArtifact.class);
            if (artifacts == null) {
                throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
            }
            for (StoredChunkArtifact artifact : artifacts) {
                if (artifact != null && artifact.chunkNo() == chunkNo) {
                    return artifact;
                }
            }
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    private long copyBinaryAndDigest(SourceFileKind kind, InputStream content, Path temporary, MessageDigest digest)
        throws IOException {
        long total = 0;
        try (BufferedInputStream input = new BufferedInputStream(content);
             DigestInputStream digestInput = new DigestInputStream(input, digest);
             OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] expected;
            if (kind == SourceFileKind.PDF) {
                expected = PDF_SIGNATURE;
            } else if (kind.zipContainer()) {
                expected = ZIP_SIGNATURE;
            } else if (kind.oleContainer()) {
                expected = OLE_SIGNATURE;
            } else {
                throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
            }
            byte[] prefix = digestInput.readNBytes(expected.length);
            if (prefix.length != expected.length || !java.util.Arrays.equals(prefix, expected)) {
                throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
            }
            output.write(prefix);
            total += prefix.length;
            total += copyRemaining(digestInput, output, total);
        }
        return total;
    }

    private long copyTextAndDigest(InputStream content, Path temporary, MessageDigest digest) throws IOException {
        long total = 0;
        try (BufferedInputStream input = new BufferedInputStream(content);
             DigestInputStream digestInput = new DigestInputStream(input, digest);
             OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = digestInput.read(buffer)) != -1) {
                total += read;
                if (total > properties.getMaxFileSize().toBytes()) {
                    throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
                }
                for (int index = 0; index < read; index++) {
                    if (buffer[index] == 0) {
                        throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
                    }
                }
                output.write(buffer, 0, read);
            }
        }
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(temporary)));
        } catch (java.nio.charset.CharacterCodingException exception) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
        }
        return total;
    }

    private long copyRemaining(InputStream input, OutputStream output, long already) throws IOException {
        long total = already;
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > properties.getMaxFileSize().toBytes()) {
                throw new BusinessException(KnowledgeErrorCode.INVALID_SOURCE_FILE);
            }
            output.write(buffer, 0, read);
        }
        return total - already;
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

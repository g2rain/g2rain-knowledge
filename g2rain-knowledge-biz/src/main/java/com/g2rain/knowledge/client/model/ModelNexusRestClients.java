package com.g2rain.knowledge.client.model;

import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.common.web.PrincipalHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Objects;

/**
 * Builds RestClient instances for internal Model Nexus OpenAI-compatible calls.
 * Propagates non-blank {@link PrincipalHeaders} from the current principal context;
 * never injects vendor provider credentials.
 */
final class ModelNexusRestClients {
    private ModelNexusRestClients() {
    }

    static RestClient create(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder()
            .requestFactory(factory)
            .requestInterceptor(principalHeadersInterceptor())
            .build();
    }

    private static ClientHttpRequestInterceptor principalHeadersInterceptor() {
        return (request, body, execution) -> {
            PrincipalContext context = PrincipalContextHolder.get();
            if (Objects.nonNull(context)) {
                for (PrincipalHeaders header : PrincipalHeaders.values()) {
                    String value = context.getValue(header);
                    if (Objects.nonNull(value) && !value.isBlank()) {
                        request.getHeaders().set(header.getUpper(), value);
                    }
                }
            }
            return execution.execute(request, body);
        };
    }
}

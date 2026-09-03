package com.lecture.material.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * 공급사 표시명을 user-service 에서 가져온다.
 * Gateway 는 X-User-Id / X-User-Email 만 주입하고 이름은 주지 않는다.
 * 조회에 실패해도 등록 자체는 막지 않는다 (이메일로 대체).
 */
@Slf4j
@Component
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder builder,
                             @Value("${service.user-service.url}") String userServiceUrl) {
        this.webClient = builder.baseUrl(userServiceUrl).build();
    }

    /** 소속(companyName)이 있으면 소속을, 없으면 이름을 공급사 표시명으로 쓴다. */
    public String resolveSupplierName(Long userId, String fallback) {
        try {
            Map<String, Object> body = webClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (body == null) return fallback;

            // 공통 래퍼({success,message,data})와 원본 객체 응답을 모두 허용한다.
            Object data = body.get("data");
            Map<?, ?> user = (data instanceof Map<?, ?> m) ? m : body;

            String companyName = str(user.get("companyName"));
            if (companyName != null) return companyName;

            String name = str(user.get("name"));
            return name != null ? name : fallback;
        } catch (Exception e) {
            log.warn("[UserServiceClient] 공급사 이름 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return fallback;
        }
    }

    private String str(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}

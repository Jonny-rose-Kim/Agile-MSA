package com.lecture.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * 구매처 표시명을 user-service 에서 가져온다.
 * 공급 공장의 "수주 관리" 화면에 구매처 이름을 보여주기 위한 것이며,
 * 조회에 실패해도 주문 생성은 막지 않는다.
 */
@Slf4j
@Component
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder builder,
                             @Value("${service.user-service.url}") String userServiceUrl) {
        this.webClient = builder.baseUrl(userServiceUrl).build();
    }

    public String resolveBuyerName(Long userId, String fallback) {
        try {
            Map<String, Object> body = webClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (body == null) return fallback;

            Object data = body.get("data");
            Map<?, ?> user = (data instanceof Map<?, ?> m) ? m : body;

            String companyName = str(user.get("companyName"));
            if (companyName != null) return companyName;

            String name = str(user.get("name"));
            return name != null ? name : fallback;
        } catch (Exception e) {
            log.warn("[UserServiceClient] 구매처 이름 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return fallback;
        }
    }

    private String str(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}

package com.lecture.material.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    /** user-service 에서 공급사 표시명(companyName)을 가져온다. 실패해도 데모가 죽지 않도록 폴백. */
    public String fetchSupplierName(Long userId) {
        try {
            Map<?, ?> res = webClientBuilder.build().get()
                    .uri("http://user-service/api/users/internal/{id}", userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Object name = res == null ? null : res.get("companyName");
            if (name == null && res != null) name = res.get("name");
            return name != null ? name.toString() : fallback(userId);
        } catch (Exception e) {
            log.warn("user-service 조회 실패 userId={}, 폴백 사용: {}", userId, e.toString());
            return fallback(userId);
        }
    }

    private String fallback(Long userId) {
        return "공급사#" + userId;
    }
}

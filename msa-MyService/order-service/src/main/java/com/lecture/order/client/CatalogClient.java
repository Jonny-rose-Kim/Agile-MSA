package com.lecture.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/** material-service / user-service 조회 (실패해도 주문 생성은 되도록 폴백) */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final WebClient.Builder webClientBuilder;

    public Map<String, Object> fetchMaterial(Long materialId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = webClientBuilder.build().get()
                    .uri("http://material-service/api/materials/internal/{id}", materialId)
                    .retrieve().bodyToMono(Map.class).block();
            return res;
        } catch (Exception e) {
            log.warn("material-service 조회 실패 materialId={}: {}", materialId, e.toString());
            return null;
        }
    }

    public String fetchUserName(Long userId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = webClientBuilder.build().get()
                    .uri("http://user-service/api/users/internal/{id}", userId)
                    .retrieve().bodyToMono(Map.class).block();
            Object name = res == null ? null : res.getOrDefault("companyName", res.get("name"));
            return name != null ? name.toString() : ("사용자#" + userId);
        } catch (Exception e) {
            return "사용자#" + userId;
        }
    }
}

package com.lecture.order.service;

import com.lecture.order.dto.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * material-service 동기 호출.
 * 주문 금액과 공급사는 반드시 여기서 받아온다 — 클라이언트가 보낸 값을 쓰면 단가를 위조할 수 있다.
 */
@Slf4j
@Component
public class MaterialServiceClient {

    private final WebClient webClient;

    public MaterialServiceClient(WebClient.Builder builder,
                                 @Value("${service.material-service.url}") String materialServiceUrl,
                                 @Value("${internal.api-key}") String internalApiKey) {
        // 내부 API 는 이 키가 있어야 열린다. (material-service 의 InternalApiInterceptor 참고)
        this.webClient = builder
                .baseUrl(materialServiceUrl)
                .defaultHeader("X-Internal-Key", internalApiKey)
                .build();
    }

    public OrderDto.MaterialSnapshot getMaterial(Long materialId) {
        try {
            Map<String, Object> body = webClient.get()
                    .uri("/api/materials/internal/{id}", materialId)
                    .retrieve()
                    .bodyToMono(MAP)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (body == null) {
                throw new IllegalArgumentException("원료 정보를 찾을 수 없습니다: " + materialId);
            }
            return toSnapshot(body);

        } catch (WebClientResponseException.NotFound | WebClientResponseException.BadRequest e) {
            throw new IllegalArgumentException("원료 정보를 찾을 수 없습니다: " + materialId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MaterialServiceClient] 원료 조회 실패 - materialId: {}, error: {}", materialId, e.getMessage());
            throw new IllegalStateException("원료 정보를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /** 여유 생산능력 차감. 부족하면 material-service 가 400 을 반환한다. */
    public void reserveCapacity(Long materialId, int quantity) {
        try {
            webClient.post()
                    .uri("/api/materials/internal/{id}/reserve", materialId)
                    .bodyValue(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();

        } catch (WebClientResponseException.BadRequest e) {
            throw new IllegalArgumentException("여유 생산능력이 부족해 조달을 신청할 수 없습니다");
        } catch (Exception e) {
            log.error("[MaterialServiceClient] 생산능력 차감 실패 - materialId: {}, error: {}",
                    materialId, e.getMessage());
            throw new IllegalStateException("원료 재고를 확보하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /** 주문 저장이 실패했을 때 차감분을 되돌린다. 보상 실패는 로그만 남긴다. */
    public void releaseCapacity(Long materialId, int quantity) {
        try {
            webClient.post()
                    .uri("/api/materials/internal/{id}/release", materialId)
                    .bodyValue(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("[MaterialServiceClient] 생산능력 복원 완료 - materialId: {}, quantity: {}", materialId, quantity);
        } catch (Exception e) {
            log.error("[MaterialServiceClient] 생산능력 복원 실패 - materialId: {}, quantity: {}, error: {}",
                    materialId, quantity, e.getMessage());
        }
    }

    private static final org.springframework.core.ParameterizedTypeReference<Map<String, Object>> MAP =
            new org.springframework.core.ParameterizedTypeReference<>() {};

    private OrderDto.MaterialSnapshot toSnapshot(Map<String, Object> body) {
        // 공통 래퍼({success,message,data})와 원본 객체 응답을 모두 허용한다.
        Object data = body.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (data instanceof Map<?, ?>) ? (Map<String, Object>) data : body;

        return OrderDto.MaterialSnapshot.builder()
                .id(asLong(m.get("id")))
                .materialCode(asString(m.get("materialCode")))
                .materialName(asString(m.get("materialName")))
                .unit(asString(m.get("unit")))
                .unitPrice(asInt(m.get("unitPrice")))
                .minOrderQuantity(asInt(m.get("minOrderQuantity")))
                .availableCapacity(asInt(m.get("availableCapacity")))
                .status(asString(m.get("status")))
                .supplierId(asLong(m.get("supplierId")))
                .supplierName(asString(m.get("supplierName")))
                .build();
    }

    private String asString(Object v) { return v == null ? null : String.valueOf(v); }
    private Integer asInt(Object v) { return v instanceof Number n ? n.intValue() : null; }
    private Long asLong(Object v) { return v instanceof Number n ? n.longValue() : null; }
}

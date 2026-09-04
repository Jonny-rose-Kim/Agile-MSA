package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * order-service 내부 API 호출.
 * 결제 금액은 반드시 여기서 받아온다 — 클라이언트가 보낸 금액을 쓰면 위조가 가능하다.
 */
@Slf4j
@Component
public class OrderServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;

    public OrderServiceClient(WebClient.Builder builder,
                              @Value("${service.order-service.url}") String orderServiceUrl,
                              @Value("${internal.api-key}") String internalApiKey) {
        // 내부 API 는 이 키가 있어야 열린다. (order-service 의 InternalApiInterceptor 참고)
        this.webClient = builder
                .baseUrl(orderServiceUrl)
                .defaultHeader("X-Internal-Key", internalApiKey)
                .build();
    }

    public PaymentDto.OrderSnapshot getOrder(Long orderId) {
        try {
            Map<String, Object> body = webClient.get()
                    .uri("/api/orders/internal/{id}", orderId)
                    .retrieve()
                    .bodyToMono(MAP)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (body == null) {
                throw new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId);
            }

            // 공통 래퍼({success,message,data})와 원본 객체 응답을 모두 허용한다.
            Object data = body.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> o = (data instanceof Map<?, ?>) ? (Map<String, Object>) data : body;

            return PaymentDto.OrderSnapshot.builder()
                    .orderId(asLong(o.get("orderId")))
                    .buyerId(asLong(o.get("buyerId")))
                    .totalAmount(asLong(o.get("totalAmount")))
                    .status(asString(o.get("status")))
                    .materialName(asString(o.get("materialName")))
                    .build();

        } catch (WebClientResponseException.NotFound | WebClientResponseException.BadRequest e) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OrderServiceClient] 주문 조회 실패 - orderId: {}, error: {}", orderId, e.getMessage());
            throw new IllegalStateException("주문 정보를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String asString(Object v) { return v == null ? null : String.valueOf(v); }
    private Long asLong(Object v) { return v instanceof Number n ? n.longValue() : null; }
}

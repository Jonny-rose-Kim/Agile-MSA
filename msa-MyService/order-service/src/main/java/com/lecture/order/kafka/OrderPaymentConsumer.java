package com.lecture.order.kafka;

import com.lecture.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 결제 완료 이벤트 수신 → 주문 확정.
 *
 * payment-service 가 결제를 마치면 order.payment.completed 를 발행하고,
 * 여기서 주문 상태를 PENDING → CONFIRMED 로 바꾼다.
 * 주문 상세 화면은 이 상태 변화를 폴링으로 감지한다.
 *
 * 동기 호출로 묶지 않는 이유: 결제는 성공했는데 주문 확정이 실패했다고 해서
 * 결제를 되돌릴 수는 없다. 두 작업을 이벤트로 끊어 각자 재시도할 수 있게 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topic.order-payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onPaymentCompleted(Map<String, Object> event) {
        Object orderIdValue = event.get("orderId");
        if (orderIdValue == null) {
            log.warn("[OrderPaymentConsumer] orderId 없는 이벤트 무시 - event: {}", event);
            return;
        }

        Long orderId = ((Number) orderIdValue).longValue();
        try {
            orderService.confirm(orderId);
            log.info("[OrderPaymentConsumer] 결제 완료 → 주문 확정 - orderId: {}, paymentId: {}",
                    orderId, event.get("paymentId"));
        } catch (IllegalArgumentException e) {
            // 이미 확정됐거나 없는 주문. 재시도해도 결과가 같으므로 흘려보낸다.
            log.warn("[OrderPaymentConsumer] 주문 확정 건너뜀 - orderId: {}, 사유: {}", orderId, e.getMessage());
        }
    }
}

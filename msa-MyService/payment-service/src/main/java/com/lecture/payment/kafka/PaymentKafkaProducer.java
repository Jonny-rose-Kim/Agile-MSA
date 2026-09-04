package com.lecture.payment.kafka;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트 발행.
 *
 * order-service 가 이 이벤트를 받아 주문을 CONFIRMED 로 바꾼다.
 * 동기 호출로 묶지 않는 이유: 결제는 성공했는데 주문 확정이 실패했다고 해서
 * 결제를 되돌릴 수는 없다. 두 작업을 끊어 각자 재시도할 수 있게 둔다.
 *
 * 강의 템플릿의 payment.completed 와 토픽을 분리했다.
 * 그쪽은 enrollment-service 가 courseId 를 기대하며 소비하고 있어 이벤트 모양이 다르다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.order-payment-completed}")
    private String topic;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event);
            log.info("[Kafka] {} 발행 - paymentId: {}, orderId: {}, amount: {}",
                    topic, event.getPaymentId(), event.getOrderId(), event.getAmount());
        } catch (Exception e) {
            // 발행에 실패해도 결제 자체는 이미 승인됐다. 주문 확정만 지연된다.
            log.error("[Kafka] {} 발행 실패 - paymentId: {}, orderId: {}, error: {}",
                    topic, event.getPaymentId(), event.getOrderId(), e.getMessage());
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentCompletedEvent {
        private Long paymentId;
        private Long orderId;
        private Long buyerId;
        private Long amount;
        private String transactionId;
        private LocalDateTime completedAt;
    }
}

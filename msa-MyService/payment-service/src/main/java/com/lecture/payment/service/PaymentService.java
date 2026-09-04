package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.kafka.PaymentKafkaProducer;
import com.lecture.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final PaymentKafkaProducer kafkaProducer;

    /**
     * POST /api/payments — 조달 주문 결제
     *
     * 1) order-service 에서 주문을 가져와 소유자·상태·중복 결제를 검증한다
     * 2) 금액은 주문의 totalAmount 를 쓴다 (클라이언트가 보낸 값은 신뢰하지 않는다)
     * 3) 승인 처리 후 order.payment.completed 를 발행한다
     *    → order-service 가 받아 주문을 CONFIRMED 로 바꾼다
     *
     * 실제 PG 연동 전이라 승인은 항상 성공하고 거래번호를 여기서 만든다.
     */
    @Transactional
    public PaymentDto.PaymentResponse pay(Long buyerId, Long orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "이미 결제된 주문입니다 (결제번호 " + existing.getId() + ")");
        });

        PaymentDto.OrderSnapshot order = orderServiceClient.getOrder(orderId);

        if (!buyerId.equals(order.getBuyerId())) {
            throw new SecurityException("본인 주문만 결제할 수 있습니다");
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException(
                    "결제 대기 상태의 주문만 결제할 수 있습니다 (현재: " + order.getStatus() + ")");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount() <= 0) {
            throw new IllegalArgumentException("주문 금액이 올바르지 않습니다");
        }

        Payment payment = Payment.builder()
                .buyerId(buyerId)
                .orderId(orderId)
                .amount(order.getTotalAmount())
                .status(Payment.Status.PENDING)
                .build();

        // 실제 PG 승인 자리. 지금은 항상 승인된다.
        payment.complete("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());

        Payment saved = paymentRepository.save(payment);
        log.info("[PaymentService] 결제 승인 - paymentId: {}, orderId: {}, buyerId: {}, amount: {}",
                saved.getId(), orderId, buyerId, saved.getAmount());

        kafkaProducer.publishPaymentCompleted(
                PaymentKafkaProducer.PaymentCompletedEvent.builder()
                        .paymentId(saved.getId())
                        .orderId(orderId)
                        .buyerId(buyerId)
                        .amount(saved.getAmount())
                        .transactionId(saved.getTransactionId())
                        .completedAt(LocalDateTime.now())
                        .build());

        return PaymentDto.PaymentResponse.from(saved);
    }

    /** GET /api/payments/my — 내 결제 내역 */
    public Page<PaymentDto.PaymentResponse> getMyPayments(Long buyerId, Pageable pageable) {
        return paymentRepository.findByBuyerId(buyerId, pageable)
                .map(PaymentDto.PaymentResponse::from);
    }

    public PaymentDto.PaymentResponse get(Long paymentId, Long buyerId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다: " + paymentId));
        if (!payment.getBuyerId().equals(buyerId)) {
            throw new SecurityException("본인 결제 내역만 조회할 수 있습니다");
        }
        return PaymentDto.PaymentResponse.from(payment);
    }
}

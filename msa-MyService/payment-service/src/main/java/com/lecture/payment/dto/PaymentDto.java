package com.lecture.payment.dto;

import com.lecture.payment.entity.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class PaymentDto {

    /** 공통 응답 래퍼. 프론트엔드의 unwrap() 이 data 만 꺼내 쓴다. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder().success(true).message("성공").data(data).build();
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }

    /** POST /api/payments — 금액은 서버가 주문에서 가져온다. 클라이언트가 보내지 않는다. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentRequest {
        @NotNull(message = "주문번호는 필수입니다")
        private Long orderId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentResponse {
        private Long paymentId;
        private Long buyerId;
        private Long orderId;
        private Long amount;
        private Payment.Status status;
        private String transactionId;
        private LocalDateTime createdAt;

        public static PaymentResponse from(Payment p) {
            return PaymentResponse.builder()
                    .paymentId(p.getId())
                    .buyerId(p.getBuyerId())
                    .orderId(p.getOrderId())
                    .amount(p.getAmount())
                    .status(p.getStatus())
                    .transactionId(p.getTransactionId())
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }

    /** order-service 응답에서 결제에 필요한 값만 뽑은 것 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderSnapshot {
        private Long orderId;
        private Long buyerId;
        private Long totalAmount;
        private String status;
        private String materialName;
    }
}

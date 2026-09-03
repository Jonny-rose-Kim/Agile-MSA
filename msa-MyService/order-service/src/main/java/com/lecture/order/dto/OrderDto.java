package com.lecture.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lecture.order.entity.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderDto {

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

    // ===== 요청 =====

    /** POST /api/orders — 단가와 공급사는 서버가 material-service 에서 조회한다. 클라이언트가 보내지 않는다. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderRequest {
        @NotNull(message = "원료 ID는 필수입니다")
        private Long materialId;

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        private Integer quantity;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate requiredDate;
    }

    // ===== 응답 =====

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderResponse {
        private Long orderId;
        private Long buyerId;
        private String buyerName;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String unit;
        private Integer unitPrice;
        private Integer quantity;
        private Long totalAmount;
        private Long supplierId;
        private String supplierName;
        private LocalDate requiredDate;
        private Order.Status status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static OrderResponse from(Order o) {
            return OrderResponse.builder()
                    .orderId(o.getId())
                    .buyerId(o.getBuyerId())
                    .buyerName(o.getBuyerName())
                    .materialId(o.getMaterialId())
                    .materialCode(o.getMaterialCode())
                    .materialName(o.getMaterialName())
                    .unit(o.getUnit())
                    .unitPrice(o.getUnitPrice())
                    .quantity(o.getQuantity())
                    .totalAmount(o.getTotalAmount())
                    .supplierId(o.getSupplierId())
                    .supplierName(o.getSupplierName())
                    .requiredDate(o.getRequiredDate())
                    .status(o.getStatus())
                    .createdAt(o.getCreatedAt())
                    .updatedAt(o.getUpdatedAt())
                    .build();
        }
    }

    /** GET /api/orders/{id}/status — 폴링 전용 경량 응답 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderStatusResponse {
        private Long orderId;
        private Order.Status status;
        private LocalDateTime updatedAt;

        public static OrderStatusResponse from(Order o) {
            return OrderStatusResponse.builder()
                    .orderId(o.getId())
                    .status(o.getStatus())
                    .updatedAt(o.getUpdatedAt())
                    .build();
        }
    }

    /** material-service 응답에서 주문에 필요한 값만 뽑은 스냅샷 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialSnapshot {
        private Long id;
        private String materialCode;
        private String materialName;
        private String unit;
        private Integer unitPrice;
        private Integer minOrderQuantity;
        private Integer availableCapacity;
        private String status;
        private Long supplierId;
        private String supplierName;
    }
}

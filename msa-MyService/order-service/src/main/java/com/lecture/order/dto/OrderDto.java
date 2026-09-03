package com.lecture.order.dto;

import com.lecture.order.entity.Order;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder().success(true).message("성공").data(data).build();
        }
        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
        public static <T> PageResponse<T> from(Page<T> p) {
            return PageResponse.<T>builder()
                    .content(p.getContent()).page(p.getNumber()).size(p.getSize())
                    .totalElements(p.getTotalElements()).totalPages(p.getTotalPages()).last(p.isLast())
                    .build();
        }
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotNull(message = "원료 ID는 필수입니다")
        private Long materialId;
        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 1 이상이어야 합니다")
        private Integer quantity;
        private LocalDate requiredDate;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderResponse {
        private Long orderId;
        private Long buyerId;
        private String buyerName;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long supplierId;
        private String supplierName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private LocalDate requiredDate;
        private Order.Status status;
        private LocalDateTime createdAt;

        public static OrderResponse from(Order o) {
            return OrderResponse.builder()
                    .orderId(o.getId())
                    .buyerId(o.getBuyerId()).buyerName(o.getBuyerName())
                    .materialId(o.getMaterialId()).materialCode(o.getMaterialCode()).materialName(o.getMaterialName())
                    .supplierId(o.getSupplierId()).supplierName(o.getSupplierName())
                    .quantity(o.getQuantity()).unitPrice(o.getUnitPrice()).totalAmount(o.getTotalAmount())
                    .requiredDate(o.getRequiredDate()).status(o.getStatus()).createdAt(o.getCreatedAt())
                    .build();
        }
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatusResponse {
        private Long orderId;
        private Order.Status status;
        public static StatusResponse from(Order o) {
            return StatusResponse.builder().orderId(o.getId()).status(o.getStatus()).build();
        }
    }
}

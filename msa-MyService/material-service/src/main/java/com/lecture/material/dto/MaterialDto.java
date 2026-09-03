package com.lecture.material.dto;

import com.lecture.material.entity.Material;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MaterialDto {

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialRequest {
        @NotBlank(message = "원료코드는 필수입니다")
        @Size(max = 50, message = "원료코드는 50자 이하여야 합니다")
        private String materialCode;

        @NotBlank(message = "원료명은 필수입니다")
        @Size(max = 200, message = "원료명은 200자 이하여야 합니다")
        private String materialName;

        private String casNumber;

        @NotNull(message = "카테고리는 필수입니다")
        private Material.Category category;

        @NotNull(message = "단위는 필수입니다")
        private Material.Unit unit;

        @NotNull(message = "단가는 필수입니다")
        @Min(value = 0, message = "단가는 0 이상이어야 합니다")
        private Integer unitPrice;

        @Min(value = 0, message = "최소 주문 수량은 0 이상이어야 합니다")
        private Integer minOrderQuantity;

        @NotNull(message = "여유 생산능력은 필수입니다")
        @Min(value = 0, message = "여유 생산능력은 0 이상이어야 합니다")
        private Integer availableCapacity;

        @NotNull(message = "리드타임은 필수입니다")
        @Min(value = 1, message = "리드타임은 1일 이상이어야 합니다")
        private Integer leadTimeDays;

        private List<String> certifications;
        private String country;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapacityRequest {
        @NotNull(message = "여유 생산능력은 필수입니다")
        @Min(value = 0, message = "여유 생산능력은 0 이상이어야 합니다")
        private Integer availableCapacity;
    }

    /** 주문 확정 시 order-service 가 호출하는 내부 요청 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReserveRequest {
        @NotNull(message = "차감 수량은 필수입니다")
        @Min(value = 1, message = "차감 수량은 1 이상이어야 합니다")
        private Integer quantity;
    }

    // ===== 응답 =====

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialResponse {
        private Long id;
        private String materialCode;
        private String materialName;
        private String casNumber;
        private Material.Category category;
        private Material.Unit unit;
        private Integer unitPrice;
        private Integer minOrderQuantity;
        private Integer availableCapacity;
        private Integer leadTimeDays;
        private List<String> certifications;
        private String country;
        private String description;
        private Material.Status status;
        private Long supplierId;
        private String supplierName;
        private LocalDateTime createdAt;

        public static MaterialResponse from(Material m) {
            return MaterialResponse.builder()
                    .id(m.getId())
                    .materialCode(m.getMaterialCode())
                    .materialName(m.getMaterialName())
                    .casNumber(m.getCasNumber())
                    .category(m.getCategory())
                    .unit(m.getUnit())
                    .unitPrice(m.getUnitPrice())
                    .minOrderQuantity(m.getMinOrderQuantity())
                    .availableCapacity(m.getAvailableCapacity())
                    .leadTimeDays(m.getLeadTimeDays())
                    .certifications(new ArrayList<>(m.getCertifications()))
                    .country(m.getCountry())
                    .description(m.getDescription())
                    .status(m.getStatus())
                    .supplierId(m.getSupplierId())
                    .supplierName(m.getSupplierName())
                    .createdAt(m.getCreatedAt())
                    .build();
        }
    }

    /** Ep-01 US1: 같은 원료코드를 공급할 수 있는 공장 한 줄 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierOption {
        private Long materialId;
        private Long supplierId;
        private String supplierName;
        private String country;
        private Integer availableCapacity;
        private Integer leadTimeDays;
        private Integer unitPrice;
        private List<String> certifications;

        public static SupplierOption from(Material m) {
            return SupplierOption.builder()
                    .materialId(m.getId())
                    .supplierId(m.getSupplierId())
                    .supplierName(m.getSupplierName())
                    .country(m.getCountry())
                    .availableCapacity(m.getAvailableCapacity())
                    .leadTimeDays(m.getLeadTimeDays())
                    .unitPrice(m.getUnitPrice())
                    .certifications(new ArrayList<>(m.getCertifications()))
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierSearchResponse {
        private String materialCode;
        private Integer totalCount;
        private List<SupplierOption> suppliers;
    }
}

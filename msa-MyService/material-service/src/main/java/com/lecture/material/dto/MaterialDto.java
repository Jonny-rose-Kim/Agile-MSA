package com.lecture.material.dto;

import com.lecture.material.entity.Material;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MaterialDto {

    // ===== 공통 응답 래퍼 =====
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

    // ===== 페이지 래퍼 (content / page / size / totalElements / totalPages / last) =====
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
                    .content(p.getContent())
                    .page(p.getNumber())
                    .size(p.getSize())
                    .totalElements(p.getTotalElements())
                    .totalPages(p.getTotalPages())
                    .last(p.isLast())
                    .build();
        }
    }

    // ===== 등록 요청 =====
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotBlank(message = "원료코드는 필수입니다")
        private String materialCode;
        @NotBlank(message = "원료명은 필수입니다")
        private String materialName;
        private String description;
        private String casNumber;
        @NotNull(message = "카테고리는 필수입니다")
        private Material.Category category;
        @NotNull(message = "단위는 필수입니다")
        private Material.Unit unit;
        @NotNull(message = "단가는 필수입니다")
        @PositiveOrZero(message = "단가는 0 이상이어야 합니다")
        private BigDecimal unitPrice;
        private Integer minOrderQuantity;
        @NotNull(message = "여유 생산능력은 필수입니다")
        @PositiveOrZero(message = "여유 생산능력은 0 이상이어야 합니다")
        private Integer availableCapacity;
        @NotNull(message = "리드타임은 필수입니다")
        @Positive(message = "리드타임은 1 이상이어야 합니다")
        private Integer leadTimeDays;
        @Builder.Default
        private List<String> certifications = new ArrayList<>();
        private String country;
    }

    // ===== 수정 요청 (전체 필드, materialCode 제외) =====
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateRequest {
        @NotBlank private String materialName;
        private String description;
        @NotNull private Material.Category category;
        @NotNull private Material.Unit unit;
        @NotNull @PositiveOrZero private BigDecimal unitPrice;
        private Integer minOrderQuantity;
        @NotNull @PositiveOrZero private Integer availableCapacity;
        @NotNull @Positive private Integer leadTimeDays;
        @Builder.Default
        private List<String> certifications = new ArrayList<>();
        private String country;
        private Material.Status status;
    }

    // ===== 생산능력만 갱신 =====
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CapacityRequest {
        @NotNull(message = "여유 생산능력은 필수입니다")
        @PositiveOrZero(message = "여유 생산능력은 0 이상이어야 합니다")
        private Integer availableCapacity;
    }

    // ===== 원료 응답 (목록·상세 공용) =====
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MaterialResponse {
        private Long id;
        private String materialCode;
        private String materialName;
        private String description;
        private String casNumber;
        private Material.Category category;
        private Material.Unit unit;
        private BigDecimal unitPrice;
        private Integer minOrderQuantity;
        private Long supplierId;
        private String supplierName;
        private String country;
        private Integer availableCapacity;
        private Integer leadTimeDays;
        private List<String> certifications;
        private Material.Status status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static MaterialResponse from(Material m) {
            return MaterialResponse.builder()
                    .id(m.getId())
                    .materialCode(m.getMaterialCode())
                    .materialName(m.getMaterialName())
                    .description(m.getDescription())
                    .casNumber(m.getCasNumber())
                    .category(m.getCategory())
                    .unit(m.getUnit())
                    .unitPrice(m.getUnitPrice())
                    .minOrderQuantity(m.getMinOrderQuantity())
                    .supplierId(m.getSupplierId())
                    .supplierName(m.getSupplierName())
                    .country(m.getCountry())
                    .availableCapacity(m.getAvailableCapacity())
                    .leadTimeDays(m.getLeadTimeDays())
                    .certifications(m.getCertifications())
                    .status(m.getStatus())
                    .createdAt(m.getCreatedAt())
                    .updatedAt(m.getUpdatedAt())
                    .build();
        }
    }

    // ===== code/{materialCode}/suppliers 응답 =====
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SupplierItem {
        private Long materialId;
        private Long supplierId;
        private String supplierName;
        private String country;
        private Integer availableCapacity;
        private Integer leadTimeDays;
        private BigDecimal unitPrice;
        private List<String> certifications;
        private boolean suppliable;

        public static SupplierItem from(Material m, Integer requiredQuantity) {
            boolean ok = requiredQuantity == null || m.getAvailableCapacity() >= requiredQuantity;
            return SupplierItem.builder()
                    .materialId(m.getId())
                    .supplierId(m.getSupplierId())
                    .supplierName(m.getSupplierName())
                    .country(m.getCountry())
                    .availableCapacity(m.getAvailableCapacity())
                    .leadTimeDays(m.getLeadTimeDays())
                    .unitPrice(m.getUnitPrice())
                    .certifications(m.getCertifications())
                    .suppliable(ok)
                    .build();
        }
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SuppliersResponse {
        private String materialCode;
        private String materialName;
        private Integer requiredQuantity;
        private List<SupplierItem> suppliers;
        private int totalCount;
    }
}

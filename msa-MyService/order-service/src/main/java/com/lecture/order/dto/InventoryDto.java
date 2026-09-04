package com.lecture.order.dto;

import com.lecture.order.entity.Inventory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class InventoryDto {

    // ===== 요청 =====

    /** POST /api/inventories — 원료명·단위는 서버가 material-service 에서 채운다. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryRequest {
        @NotBlank(message = "원료코드는 필수입니다")
        private String materialCode;

        @NotNull(message = "현재 재고량은 필수입니다")
        @Min(value = 0, message = "현재 재고량은 0 이상이어야 합니다")
        private Integer currentStock;

        @NotNull(message = "부족 임계치는 필수입니다")
        @Min(value = 0, message = "부족 임계치는 0 이상이어야 합니다")
        private Integer threshold;
    }

    /** PATCH /api/inventories/{id}/stock — 입고·소진 반영 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockUpdateRequest {
        @NotNull(message = "현재 재고량은 필수입니다")
        @Min(value = 0, message = "현재 재고량은 0 이상이어야 합니다")
        private Integer currentStock;
    }

    // ===== 응답 =====

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryResponse {
        private Long inventoryId;
        private String materialCode;
        private String materialName;
        private String unit;
        private Integer currentStock;
        private Integer threshold;
        private Integer shortageQuantity;
        private boolean shortage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static InventoryResponse from(Inventory i) {
            return InventoryResponse.builder()
                    .inventoryId(i.getId())
                    .materialCode(i.getMaterialCode())
                    .materialName(i.getMaterialName())
                    .unit(i.getUnit())
                    .currentStock(i.getCurrentStock())
                    .threshold(i.getThreshold())
                    .shortageQuantity(i.shortageQuantity())
                    .shortage(i.isShort())
                    .createdAt(i.getCreatedAt())
                    .updatedAt(i.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 부족 알림 한 건.
     *
     * severity 는 부족량이 임계치에서 차지하는 비율로 정한다.
     * 원료마다 단위와 사용 규모가 달라 절대량으로는 비교가 되지 않기 때문이다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertResponse {
        private Long inventoryId;
        private String severity; // CRITICAL | HIGH | MEDIUM
        private String materialCode;
        private String materialName;
        private String unit;
        private Integer currentStock;
        private Integer threshold;
        private Integer shortageQuantity;
        private LocalDateTime detectedAt;

        public static AlertResponse from(Inventory i, LocalDateTime detectedAt) {
            return AlertResponse.builder()
                    .inventoryId(i.getId())
                    .severity(severityOf(i))
                    .materialCode(i.getMaterialCode())
                    .materialName(i.getMaterialName())
                    .unit(i.getUnit())
                    .currentStock(i.getCurrentStock())
                    .threshold(i.getThreshold())
                    .shortageQuantity(i.shortageQuantity())
                    .detectedAt(detectedAt)
                    .build();
        }

        private static String severityOf(Inventory i) {
            if (i.getCurrentStock() <= 0) {
                return "CRITICAL";                       // 이미 소진됨
            }
            if (i.getThreshold() > 0 && i.shortageQuantity() * 2 >= i.getThreshold()) {
                return "HIGH";                           // 임계치의 절반 이상 모자람
            }
            return "MEDIUM";
        }
    }

    /** GET /api/inventories/alerts — 화면이 alerts 배열만 읽는다. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertListResponse {
        private Integer totalCount;
        private List<AlertResponse> alerts;

        public static AlertListResponse of(List<AlertResponse> alerts) {
            return AlertListResponse.builder()
                    .totalCount(alerts.size())
                    .alerts(alerts)
                    .build();
        }
    }
}

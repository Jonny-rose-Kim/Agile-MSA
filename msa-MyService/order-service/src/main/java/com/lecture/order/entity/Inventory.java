package com.lecture.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 수요자(제약사·연구실)가 보유한 원료 재고와 부족 임계치.
 *
 * 원료를 materialCode 로 잡는다. 같은 성분이라도 공급 공장마다 다른 materials 행이 되는데,
 * 재고는 "우리가 이 성분을 얼마나 갖고 있는가"의 문제라 특정 공장 상품에 묶이지 않는다.
 * 원료명·단위는 등록 시점 값을 함께 저장한다(Order 와 같은 스냅샷 방식).
 */
@Entity
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(name = "uq_inventory_buyer_material",
                columnNames = {"buyer_id", "material_code"}),
        indexes = @Index(name = "idx_inventories_buyer", columnList = "buyer_id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    /** 등록 시점 스냅샷. 카탈로그에 없는 원료도 등록할 수 있어 null 을 허용한다. */
    @Column(name = "material_name", length = 200)
    private String materialName;

    @Column(length = 10)
    private String unit;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    /** 부족 판정 기준. 향후 recommend-service 가 외부 지표를 반영해 갱신한다. */
    @Column(nullable = false)
    private Integer threshold;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 임계치 미만이면 부족. 같으면 아직 부족이 아니다. */
    public boolean isShort() {
        return currentStock < threshold;
    }

    /** 임계치까지 채우는 데 모자란 양 */
    public int shortageQuantity() {
        return Math.max(0, threshold - currentStock);
    }

    public void updateStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public void updateThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public void applySnapshot(String materialName, String unit) {
        this.materialName = materialName;
        this.unit = unit;
    }

    public boolean isOwnedBy(Long userId) {
        return this.buyerId.equals(userId);
    }
}

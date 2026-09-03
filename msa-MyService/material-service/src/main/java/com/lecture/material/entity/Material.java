package com.lecture.material.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 공급 원료.
 * supplierId 는 Gateway 가 주입한 X-User-Id 로만 채운다. 클라이언트가 보내는 값은 신뢰하지 않는다.
 */
@Entity
@Table(name = "materials", indexes = {
        @Index(name = "idx_materials_code", columnList = "material_code"),
        @Index(name = "idx_materials_supplier", columnList = "supplier_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 원료코드. 같은 원료를 여러 공장이 공급하므로 전역 유일이 아니다. */
    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 200)
    private String materialName;

    @Column(name = "cas_number", length = 50)
    private String casNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Unit unit;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "min_order_quantity")
    private Integer minOrderQuantity;

    /** 여유 생산능력. 조달 주문이 확정되면 그만큼 차감된다. (Ep-02 US2) */
    @Column(name = "available_capacity", nullable = false)
    private Integer availableCapacity;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "material_certifications", joinColumns = @JoinColumn(name = "material_id"))
    @Column(name = "certification", length = 20)
    @Builder.Default
    private List<String> certifications = new ArrayList<>();

    @Column(length = 50)
    private String country;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Category { API_INGREDIENT, EXCIPIENT, SOLVENT, REAGENT, INTERMEDIATE, OTHER }

    public enum Unit { KG, L, EA }

    public enum Status { ACTIVE, INACTIVE }

    // --- 상태 변경 ---

    public void update(String materialName, String casNumber, Category category, Unit unit,
                       Integer unitPrice, Integer minOrderQuantity, Integer availableCapacity,
                       Integer leadTimeDays, List<String> certifications,
                       String country, String description) {
        this.materialName = materialName;
        this.casNumber = casNumber;
        this.category = category;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.minOrderQuantity = minOrderQuantity;
        this.availableCapacity = availableCapacity;
        this.leadTimeDays = leadTimeDays;
        this.certifications = certifications == null ? new ArrayList<>() : new ArrayList<>(certifications);
        this.country = country;
        this.description = description;
    }

    public void changeCapacity(Integer availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    /** 조달 주문 확정 시 여유 생산능력을 차감한다. */
    public void reserveCapacity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다");
        }
        if (this.availableCapacity < quantity) {
            throw new IllegalArgumentException(
                    "여유 생산능력이 부족합니다 (요청 " + quantity + ", 가용 " + this.availableCapacity + ")");
        }
        this.availableCapacity -= quantity;
    }

    /** 주문 생성이 실패했을 때 차감분을 되돌린다. */
    public void releaseCapacity(int quantity) {
        this.availableCapacity += Math.max(quantity, 0);
    }

    /** 공급 중단 (Soft Delete) */
    public void deactivate() {
        this.status = Status.INACTIVE;
    }

    public boolean isOwnedBy(Long userId) {
        return this.supplierId.equals(userId);
    }
}

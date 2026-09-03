package com.lecture.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 조달 주문.
 *
 * 원료명·단가·공급사를 주문 시점 값으로 함께 저장한다(스냅샷).
 * 공급사가 나중에 단가를 바꾸거나 공급을 중단해도 과거 주문 내역은 그대로 남아야 하고,
 * 목록을 그릴 때마다 material-service 를 다시 호출하지 않기 위해서다.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_buyer", columnList = "buyer_id"),
        @Index(name = "idx_orders_supplier", columnList = "supplier_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    // --- 주문 시점 스냅샷 ---
    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 200)
    private String materialName;

    @Column(length = 10)
    private String unit;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /** 단가 × 수량. int 범위를 넘길 수 있어 Long 으로 둔다. */
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,    // 조달 신청 완료, 결제 대기
        CONFIRMED,  // 결제 완료
        CANCELLED,  // 취소
        FAILED      // 처리 실패
    }

    public void confirm() {
        this.status = Status.CONFIRMED;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    /** 주문 당사자(구매처 또는 공급 공장)만 상세를 볼 수 있다. */
    public boolean isVisibleTo(Long userId) {
        return this.buyerId.equals(userId) || this.supplierId.equals(userId);
    }
}

package com.lecture.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 조달 주문 결제.
 *
 * 테이블명이 order_payments 인 이유: 강의 결제(payments, course_id NOT NULL)와 도메인이 다르다.
 * 같은 테이블을 재사용하면 기존 볼륨을 쓰는 사람은 course_id 제약에 걸려 저장이 실패한다.
 * 새 테이블로 두면 마이그레이션 없이 누구나 그대로 뜬다.
 */
@Entity
@Table(name = "order_payments", indexes = {
        @Index(name = "idx_order_payments_buyer", columnList = "buyer_id"),
        @Index(name = "idx_order_payments_order", columnList = "order_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    /** 한 주문은 한 번만 결제된다. */
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /** PG 승인번호. 실제 PG 연동 전이라 서비스에서 생성한다. */
    @Column(name = "transaction_id", unique = true, length = 60)
    private String transactionId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,    // 승인 요청 전
        COMPLETED,  // 승인 완료
        FAILED      // 승인 실패
    }

    public void complete(String transactionId) {
        this.status = Status.COMPLETED;
        this.transactionId = transactionId;
    }

    public void fail() {
        this.status = Status.FAILED;
    }
}

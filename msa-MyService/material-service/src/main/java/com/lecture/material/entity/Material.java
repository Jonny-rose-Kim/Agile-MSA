package com.lecture.material.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String materialCode;

    @Column(nullable = false)
    private String materialName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String casNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    private Integer minOrderQuantity;

    /** 공급사(user) ID. X-User-Id 로 세팅. */
    @Column(nullable = false)
    private Long supplierId;

    /** user-service 에서 조회해 비정규화 저장 (스크린샷/데모 안정성). */
    private String supplierName;

    private String country;

    @Column(nullable = false)
    private Integer availableCapacity;

    @Column(nullable = false)
    private Integer leadTimeDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "material_certifications", joinColumns = @JoinColumn(name = "material_id"))
    @Column(name = "certification")
    @Builder.Default
    private List<String> certifications = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Category {
        API_INGREDIENT, EXCIPIENT, SOLVENT, REAGENT, INTERMEDIATE, OTHER
    }

    public enum Unit {
        KG, L, EA
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
}

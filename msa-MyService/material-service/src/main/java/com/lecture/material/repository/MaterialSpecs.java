package com.lecture.material.repository;

import com.lecture.material.entity.Material;
import org.springframework.data.jpa.domain.Specification;

/**
 * 검색 조건 조립.
 * JPQL 의 ":param IS NULL" 패턴은 Hibernate 버전에 따라 타입 추론이 흔들려서
 * Criteria API 로 조건을 붙인다. 조건이 없으면 절을 아예 만들지 않는다.
 */
public final class MaterialSpecs {

    private MaterialSpecs() {}

    public static Specification<Material> status(Material.Status status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Material> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("materialName")), like),
                cb.like(cb.lower(root.get("materialCode")), like),
                cb.like(cb.lower(cb.coalesce(root.get("casNumber"), "")), like)
        );
    }

    public static Specification<Material> category(Material.Category category) {
        if (category == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Material> minCapacity(Integer minCapacity) {
        if (minCapacity == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("availableCapacity"), minCapacity);
    }

    public static Specification<Material> certification(String certification) {
        if (certification == null || certification.isBlank()) return null;
        return (root, query, cb) -> cb.isMember(certification.trim().toUpperCase(), root.get("certifications"));
    }

    public static Specification<Material> materialCode(String materialCode) {
        if (materialCode == null || materialCode.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("materialCode"), materialCode);
    }
}

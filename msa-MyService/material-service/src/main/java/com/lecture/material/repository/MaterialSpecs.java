package com.lecture.material.repository;

import com.lecture.material.entity.Material;
import org.springframework.data.jpa.domain.Specification;

public final class MaterialSpecs {

    private MaterialSpecs() {}

    public static Specification<Material> statusEq(Material.Status status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Material> categoryEq(Material.Category category) {
        return (root, q, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Material> keyword(String keyword) {
        return (root, q, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("materialName")), like),
                    cb.like(cb.lower(root.get("materialCode")), like),
                    cb.like(cb.lower(root.get("casNumber")), like)
            );
        };
    }

    public static Specification<Material> minCapacity(Integer minCapacity) {
        return (root, q, cb) -> minCapacity == null ? null
                : cb.greaterThanOrEqualTo(root.get("availableCapacity"), minCapacity);
    }

    public static Specification<Material> hasCertification(String certification) {
        return (root, q, cb) -> (certification == null || certification.isBlank())
                ? null
                : cb.isMember(certification, root.get("certifications"));
    }
}

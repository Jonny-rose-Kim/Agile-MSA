package com.lecture.material.repository;

import com.lecture.material.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {

    Page<Material> findBySupplierId(Long supplierId, Pageable pageable);

    boolean existsByMaterialCodeAndSupplierId(String materialCode, Long supplierId);
}

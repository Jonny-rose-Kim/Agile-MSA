package com.lecture.order.repository;

import com.lecture.order.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Page<Inventory> findByBuyerId(Long buyerId, Pageable pageable);

    boolean existsByBuyerIdAndMaterialCode(Long buyerId, String materialCode);

    /** 특정 수요자의 부족 재고 — 알림 조회용 */
    @Query("SELECT i FROM Inventory i WHERE i.buyerId = :buyerId AND i.currentStock < i.threshold")
    List<Inventory> findShortagesByBuyerId(Long buyerId);

    /** 전체 부족 재고 — 감지 스케줄러용 */
    @Query("SELECT i FROM Inventory i WHERE i.currentStock < i.threshold")
    List<Inventory> findAllShortages();
}

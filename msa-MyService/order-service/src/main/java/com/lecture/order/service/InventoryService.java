package com.lecture.order.service;

import com.lecture.order.dto.InventoryDto;
import com.lecture.order.dto.OrderDto;
import com.lecture.order.entity.Inventory;
import com.lecture.order.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 보유 재고와 부족 임계치 관리 (Ep-02 US1).
 *
 * 재고는 수요자 본인 것만 다룬다. buyerId 는 Gateway 가 주입한 X-User-Id 에서만 온다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MaterialServiceClient materialServiceClient;

    @Transactional
    public InventoryDto.InventoryResponse create(Long buyerId, InventoryDto.InventoryRequest request) {
        String materialCode = request.getMaterialCode().trim();

        if (inventoryRepository.existsByBuyerIdAndMaterialCode(buyerId, materialCode)) {
            throw new IllegalArgumentException("이미 등록한 원료코드입니다: " + materialCode);
        }

        Inventory inventory = Inventory.builder()
                .buyerId(buyerId)
                .materialCode(materialCode)
                .currentStock(request.getCurrentStock())
                .threshold(request.getThreshold())
                .build();

        // 카탈로그에 있으면 원료명·단위를 채운다. 없어도 재고 등록 자체는 막지 않는다.
        OrderDto.MaterialSnapshot snapshot = materialServiceClient.findByCode(materialCode);
        if (snapshot != null) {
            inventory.applySnapshot(snapshot.getMaterialName(), snapshot.getUnit());
        } else {
            log.info("[InventoryService] 카탈로그에 없는 원료코드로 재고 등록 - buyerId: {}, materialCode: {}",
                    buyerId, materialCode);
        }

        Inventory saved = inventoryRepository.save(inventory);
        log.info("[InventoryService] 재고 등록 - inventoryId: {}, buyerId: {}, materialCode: {}, 현재고: {}, 임계치: {}",
                saved.getId(), buyerId, materialCode, saved.getCurrentStock(), saved.getThreshold());

        return InventoryDto.InventoryResponse.from(saved);
    }

    public Page<InventoryDto.InventoryResponse> getMyInventories(Long buyerId, Pageable pageable) {
        return inventoryRepository.findByBuyerId(buyerId, pageable)
                .map(InventoryDto.InventoryResponse::from);
    }

    @Transactional
    public InventoryDto.InventoryResponse updateStock(Long inventoryId, Long buyerId, Integer currentStock) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("재고 정보를 찾을 수 없습니다: " + inventoryId));

        if (!inventory.isOwnedBy(buyerId)) {
            throw new SecurityException("본인의 재고만 수정할 수 있습니다");
        }

        inventory.updateStock(currentStock);
        log.info("[InventoryService] 재고 수량 변경 - inventoryId: {}, 현재고: {} (임계치: {})",
                inventoryId, currentStock, inventory.getThreshold());

        return InventoryDto.InventoryResponse.from(inventory);
    }

    /** 부족 알림 조회. 스케줄러가 감지하는 기준과 동일하다. */
    public InventoryDto.AlertListResponse getAlerts(Long buyerId) {
        LocalDateTime detectedAt = LocalDateTime.now();

        List<InventoryDto.AlertResponse> alerts = inventoryRepository.findShortagesByBuyerId(buyerId).stream()
                .map(inventory -> InventoryDto.AlertResponse.from(inventory, detectedAt))
                .toList();

        return InventoryDto.AlertListResponse.of(alerts);
    }
}

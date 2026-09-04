package com.lecture.order.scheduler;

import com.lecture.order.entity.Inventory;
import com.lecture.order.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 재고 부족 감지 스케줄러 (Ep-02 US1).
 *
 * inventories 를 주기적으로 훑어 current_stock < threshold 인 항목을 찾아 알린다.
 * 담당자는 알림을 보고 POST /api/orders 로 조달을 신청한다.
 *
 * 지금은 감지 결과를 로그로만 남긴다. 화면은 GET /api/inventories/alerts 로 같은 기준의
 * 결과를 조회한다. 알림 채널(메일·슬랙 등)이 정해지면 이 자리에서 발송하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryShortageScheduler {

    private final InventoryRepository inventoryRepository;

    @Scheduled(fixedDelayString = "${inventory.shortage-check.interval-ms:60000}")
    public void detectShortages() {
        List<Inventory> shortages = inventoryRepository.findAllShortages();

        if (shortages.isEmpty()) {
            log.debug("[InventoryShortageScheduler] 부족 원료 없음");
            return;
        }

        log.info("[InventoryShortageScheduler] 재고 부족 {}건 감지", shortages.size());
        for (Inventory i : shortages) {
            log.warn("[InventoryShortageScheduler] 재고 부족 - inventoryId: {}, buyerId: {}, 원료: {}({}), " +
                            "현재고: {}, 임계치: {}, 부족량: {} → 조달 신청 필요",
                    i.getId(), i.getBuyerId(),
                    i.getMaterialName() != null ? i.getMaterialName() : "-", i.getMaterialCode(),
                    i.getCurrentStock(), i.getThreshold(), i.shortageQuantity());
        }
    }
}

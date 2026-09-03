package com.lecture.order.service;

import com.lecture.order.dto.OrderDto;
import com.lecture.order.entity.Order;
import com.lecture.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MaterialServiceClient materialServiceClient;
    private final UserServiceClient userServiceClient;

    /**
     * POST /api/orders — 조달 신청
     *
     * 1) material-service 에서 원료 정보를 가져와 검증한다
     * 2) 여유 생산능력을 차감한다 (실제로 댈 수 있는 물량만 주문되도록)
     * 3) 주문 시점 단가·공급사를 스냅샷으로 저장하고 PENDING 으로 만든다
     *
     * 3)이 실패하면 2)를 되돌린다. 결제는 Sprint 2 에서 붙고, 그때 CONFIRMED 로 바뀐다.
     */
    @Transactional
    public OrderDto.OrderResponse create(Long buyerId, String buyerEmail, OrderDto.OrderRequest request) {
        OrderDto.MaterialSnapshot material = materialServiceClient.getMaterial(request.getMaterialId());

        validate(material, request.getQuantity());

        if (material.getSupplierId().equals(buyerId)) {
            throw new IllegalArgumentException("본인이 공급하는 원료는 조달 신청할 수 없습니다");
        }

        materialServiceClient.reserveCapacity(material.getId(), request.getQuantity());

        try {
            long totalAmount = (long) material.getUnitPrice() * request.getQuantity();

            Order order = Order.builder()
                    .buyerId(buyerId)
                    .buyerName(userServiceClient.resolveBuyerName(buyerId, buyerEmail))
                    .materialId(material.getId())
                    .materialCode(material.getMaterialCode())
                    .materialName(material.getMaterialName())
                    .unit(material.getUnit())
                    .unitPrice(material.getUnitPrice())
                    .quantity(request.getQuantity())
                    .totalAmount(totalAmount)
                    .supplierId(material.getSupplierId())
                    .supplierName(material.getSupplierName())
                    .requiredDate(request.getRequiredDate())
                    .status(Order.Status.PENDING)
                    .build();

            Order saved = orderRepository.save(order);
            log.info("[OrderService] 조달 신청 - orderId: {}, buyerId: {}, materialId: {}, quantity: {}, amount: {}",
                    saved.getId(), buyerId, material.getId(), request.getQuantity(), totalAmount);
            return OrderDto.OrderResponse.from(saved);

        } catch (RuntimeException e) {
            // 주문 저장이 실패했으면 차감한 생산능력을 돌려놓는다.
            materialServiceClient.releaseCapacity(material.getId(), request.getQuantity());
            throw e;
        }
    }

    /** GET /api/orders/my — 구매처 본인 주문 */
    public Page<OrderDto.OrderResponse> getMyOrders(Long buyerId, Pageable pageable) {
        return orderRepository.findByBuyerId(buyerId, pageable).map(OrderDto.OrderResponse::from);
    }

    /** GET /api/orders/supplier — 공급 공장이 받은 주문 (Ep-01 US2) */
    public Page<OrderDto.OrderResponse> getSupplierOrders(Long supplierId, Order.Status status, Pageable pageable) {
        Page<Order> page = (status == null)
                ? orderRepository.findBySupplierId(supplierId, pageable)
                : orderRepository.findBySupplierIdAndStatus(supplierId, status, pageable);
        return page.map(OrderDto.OrderResponse::from);
    }

    /** GET /api/orders/{id} — 주문 당사자만 볼 수 있다. */
    public OrderDto.OrderResponse get(Long orderId, Long userId) {
        return OrderDto.OrderResponse.from(findVisible(orderId, userId));
    }

    /** GET /api/orders/{id}/status — 폴링용 경량 조회 */
    public OrderDto.OrderStatusResponse getStatus(Long orderId, Long userId) {
        return OrderDto.OrderStatusResponse.from(findVisible(orderId, userId));
    }

    /**
     * 결제 완료 처리. Sprint 2 에서 payment-service 가 결제를 마치면 호출한다.
     * (Kafka payment.completed 소비는 payment-service 담당자가 이어서 붙인다)
     */
    @Transactional
    public OrderDto.OrderResponse confirm(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        if (order.getStatus() != Order.Status.PENDING) {
            throw new IllegalArgumentException("결제 대기 상태의 주문만 확정할 수 있습니다 (현재: " + order.getStatus() + ")");
        }
        order.confirm();
        log.info("[OrderService] 주문 확정 - orderId: {}", orderId);
        return OrderDto.OrderResponse.from(order);
    }

    // ===== 내부 유틸 =====

    private void validate(OrderDto.MaterialSnapshot material, int quantity) {
        if (!"ACTIVE".equalsIgnoreCase(material.getStatus())) {
            throw new IllegalArgumentException("공급이 중단된 원료입니다");
        }
        Integer minOrder = material.getMinOrderQuantity();
        if (minOrder != null && quantity < minOrder) {
            throw new IllegalArgumentException(
                    "최소 주문 수량은 " + minOrder + " " + material.getUnit() + " 입니다");
        }
        Integer capacity = material.getAvailableCapacity();
        if (capacity != null && quantity > capacity) {
            throw new IllegalArgumentException(
                    "여유 생산능력을 초과했습니다 (요청 " + quantity + ", 가용 " + capacity + ")");
        }
    }

    private Order findVisible(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        if (!order.isVisibleTo(userId)) {
            throw new SecurityException("본인 주문만 조회할 수 있습니다");
        }
        return order;
    }
}

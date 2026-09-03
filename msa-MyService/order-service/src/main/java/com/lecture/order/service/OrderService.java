package com.lecture.order.service;

import com.lecture.order.client.CatalogClient;
import com.lecture.order.dto.OrderDto;
import com.lecture.order.entity.Order;
import com.lecture.order.exception.ApiException;
import com.lecture.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository repository;
    private final CatalogClient catalogClient;

    public OrderDto.PageResponse<OrderDto.OrderResponse> findMine(Long buyerId, Pageable pageable) {
        Page<OrderDto.OrderResponse> page = repository.findByBuyerId(buyerId, pageable)
                .map(OrderDto.OrderResponse::from);
        return OrderDto.PageResponse.from(page);
    }

    public OrderDto.PageResponse<OrderDto.OrderResponse> findForSupplier(Long supplierId, Order.Status status, Pageable pageable) {
        Page<Order> page = (status == null)
                ? repository.findBySupplierId(supplierId, pageable)
                : repository.findBySupplierIdAndStatus(supplierId, status, pageable);
        return OrderDto.PageResponse.from(page.map(OrderDto.OrderResponse::from));
    }

    public OrderDto.OrderResponse get(Long id) {
        return OrderDto.OrderResponse.from(findById(id));
    }

    public OrderDto.StatusResponse getStatus(Long id) {
        return OrderDto.StatusResponse.from(findById(id));
    }

    @Transactional
    public OrderDto.OrderResponse create(OrderDto.CreateRequest req, Long buyerId) {
        Order.OrderBuilder b = Order.builder()
                .buyerId(buyerId)
                .buyerName(catalogClient.fetchUserName(buyerId))
                .materialId(req.getMaterialId())
                .quantity(req.getQuantity())
                .requiredDate(req.getRequiredDate())
                .status(Order.Status.PENDING);

        Map<String, Object> m = catalogClient.fetchMaterial(req.getMaterialId());
        if (m == null) {
            throw ApiException.badRequest("존재하지 않는 원료입니다: " + req.getMaterialId());
        }
        b.materialCode(str(m.get("materialCode")))
         .materialName(str(m.get("materialName")))
         .supplierId(lng(m.get("supplierId")))
         .supplierName(str(m.get("supplierName")));

        BigDecimal unitPrice = m.get("unitPrice") == null
                ? BigDecimal.ZERO : new BigDecimal(m.get("unitPrice").toString());
        b.unitPrice(unitPrice)
         .totalAmount(unitPrice.multiply(BigDecimal.valueOf(req.getQuantity())));

        return OrderDto.OrderResponse.from(repository.save(b.build()));
    }

    private Order findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 주문입니다: " + id));
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static Long lng(Object o) { return o == null ? null : Long.valueOf(o.toString().split("\\.")[0]); }
}

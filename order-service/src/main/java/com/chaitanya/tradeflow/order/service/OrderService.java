package com.chaitanya.tradeflow.order.service;

import com.chaitanya.tradeflow.order.domain.Order;
import com.chaitanya.tradeflow.order.domain.OrderItem;
import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderStatus;
import com.chaitanya.tradeflow.order.repo.OrderRepository;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import com.chaitanya.tradeflow.order.web.dto.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;


@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private static final BigDecimal LARGE_NOTIONAL = new BigDecimal("100000");

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::create)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByAccountId(Long accountId, Pageable pageable){
        return orderRepository.findByAccountId(accountId,pageable).map(OrderResponse::create);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest orderRequest) {
        Order saved = orderRepository.save(createOrderFromOrderRequest(orderRequest));
        return OrderResponse.create(saved);
    }

    private Order createOrderFromOrderRequest(CreateOrderRequest orderRequest) {
       Order order = new Order(orderRequest.accountId());
       orderRequest.items().forEach(i -> order.addItem(new OrderItem(i.symbol(), i.quantity(), i.price(), i.side())));
        return order;
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse summary(Long accountId){
        List<Order> orders = orderRepository.findByAccountId(accountId);
        List<OrderItem> items = orders.stream().flatMap(order -> order.getItems().stream()).toList();
        Map<OrderStatus,Long> byStatus = orders.stream().collect(groupingBy(Order::getStatus,counting()));
        Map<String, BigDecimal> bySymbol = items.stream().collect(groupingBy(OrderItem::getSymbol,reducing(BigDecimal.ZERO, OrderItem::notional, BigDecimal::add)));
        Map<OrderSide,Long> bySide = items.stream().collect(groupingBy(OrderItem::getSide,counting()));
        Map<Boolean, Long> byLargeItems = items.stream().collect(partitioningBy(orderItem -> orderItem.notional().compareTo(LARGE_NOTIONAL)>=0,counting()));
        return new OrderSummaryResponse(accountId,(long)orders.size(),byStatus,bySymbol,bySide,byLargeItems);
    }
}

package com.chaitanya.tradeflow.order.service;

import com.chaitanya.tradeflow.order.domain.*;
import com.chaitanya.tradeflow.order.event.OrderCreatedEvent;
import com.chaitanya.tradeflow.order.repo.OrderRepository;
import com.chaitanya.tradeflow.order.repo.OutboxEventRepository;
import com.chaitanya.tradeflow.order.repo.ProcessedEventRepository;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import com.chaitanya.tradeflow.order.web.dto.OrderSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.*;


@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private static final BigDecimal LARGE_NOTIONAL = new BigDecimal("100000");
    private static final String ORDERS_CREATED_TOPIC = "orders.created";
    private final ProcessedEventRepository processedEventRepository;
    private static final Logger log =LoggerFactory.getLogger(OrderService.class);
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;      // import tools.jackson.databind.json.JsonMapper

    public OrderService(OrderRepository orderRepository, ProcessedEventRepository processedEventRepository,
                        OutboxEventRepository outboxEventRepository,JsonMapper jsonMapper) {
        this.orderRepository = orderRepository;
        this.processedEventRepository =  processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
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
        String payload = jsonMapper.writeValueAsString(OrderCreatedEvent.from(saved));
        OutboxEvent outboxEvent = new OutboxEvent(ORDERS_CREATED_TOPIC,String.valueOf(saved.getOrderId()),payload);
        outboxEventRepository.save(outboxEvent);
        return OrderResponse.create(saved);
    }

    private Order createOrderFromOrderRequest(CreateOrderRequest orderRequest) {
       Order order = new Order(orderRequest.accountId());
       orderRequest.items().forEach(i -> order.addItem(new OrderItem(i.symbol(), i.quantity(), i.price(), i.side())));
        return order;
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse summary(Long accountId) {
        List<Order> orders = orderRepository.findByAccountId(accountId);
        List<OrderItem> items = orders.stream().flatMap(order -> order.getItems().stream()).toList();

        Map<OrderStatus, Long> byStatus = orders.stream().collect(groupingBy(Order::getStatus, counting()));

        Map<String, BigDecimal> bySymbol = items.stream().collect(groupingBy(OrderItem::getSymbol,
                reducing(BigDecimal.ZERO, OrderItem::notional, BigDecimal::add)));

        Map<OrderSide, Long> bySide = items.stream().collect(groupingBy(OrderItem::getSide, counting()));

        Map<Boolean, Long> byLargeItems = items.stream().collect(partitioningBy(
                orderItem -> orderItem.notional().compareTo(LARGE_NOTIONAL) >= 0, counting()));

        return new OrderSummaryResponse(accountId,
                (long) orders.size(), byStatus, bySymbol, bySide, byLargeItems);
    }

    @Transactional
    public void applyRiskDecision(UUID eventId, Long orderId, boolean approved, String reason) {
        if(processedEventRepository.existsById(eventId)){
            log.info("Duplicate event {} for order {}, skipping", eventId, orderId);
            return;
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (approved) {
            order.approve();
        } else {
            order.reject(reason);
        }
        processedEventRepository.save(new ProcessedEvent(eventId));
    }
}

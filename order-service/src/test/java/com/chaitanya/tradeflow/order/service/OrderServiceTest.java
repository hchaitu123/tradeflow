package com.chaitanya.tradeflow.order.service;

import com.chaitanya.tradeflow.order.domain.Order;
import com.chaitanya.tradeflow.order.domain.OrderItem;
import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderStatus;
import com.chaitanya.tradeflow.order.event.OrderCreatedEvent;
import com.chaitanya.tradeflow.order.repo.OrderRepository;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderItemRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import com.chaitanya.tradeflow.order.web.dto.OrderSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    KafkaTemplate<String,Object> kafkaTemplate;

    @InjectMocks
    OrderService orderService;

    @Test
    void findBy_orderId_throwsNotFound(){
        when(orderRepository.findById(29L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class,() -> orderService.findById(29L));
    }

    @Test
    void createOrder_setsStatusPendingRisk() {
        // Arrange: when save() is called with any Order, return that same order back
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateOrderRequest request = new CreateOrderRequest(123L, List.of(new OrderItemRequest("INFY", 10, new BigDecimal("1520.50"), OrderSide.BUY)));

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertEquals(OrderStatus.PENDING_RISK, response.status());
        assertEquals("INFY", response.items().getFirst().symbol());
        assertEquals(1, response.items().size());
        verify(kafkaTemplate).send(eq("orders.created"), any(),any(OrderCreatedEvent.class));
    }

    @Test
    void findById_whenOrderExists_returnsOrder(){
        Order order = new Order(123L);
        order.addItem(new OrderItem("INFY", 10, new BigDecimal("1520.50"), OrderSide.BUY));
        when(orderRepository.findById(29L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.findById(29L);

        assertEquals(OrderStatus.PENDING_RISK, response.status());
        assertEquals("INFY", response.items().getFirst().symbol());
    }

    @Test
    void summary_groupsOrdersAndItems() {
        // order 1: still PENDING_RISK
        Order o1 = new Order(123L);
        o1.addItem(new OrderItem("INFY", 10, new BigDecimal("1500.00"), OrderSide.BUY));       // 15,000
        o1.addItem(new OrderItem("RELIANCE", 50, new BigDecimal("2500.00"), OrderSide.SELL));  // 1,25,000 → large

        // order 2: APPROVED
        Order o2 = new Order(123L);
        o2.addItem(new OrderItem("INFY", 5, new BigDecimal("1500.00"), OrderSide.BUY));        // 7,500
        o2.approve();

        when(orderRepository.findByAccountId(123L)).thenReturn(List.of(o1, o2));

        OrderSummaryResponse s = orderService.summary(123L);

        assertEquals(2L, s.totalOrders());
        assertEquals(1L, s.ordersByStatus().get(OrderStatus.APPROVED));
        assertEquals(2L, s.itemsBySide().get(OrderSide.BUY));
        assertEquals(1L, s.largeItems().get(true));
        assertEquals(0, new BigDecimal("22500").compareTo(s.notionalBySymbol().get("INFY")));
    }
}
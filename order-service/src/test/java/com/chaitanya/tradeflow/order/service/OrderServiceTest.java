package com.chaitanya.tradeflow.order.service;

import com.chaitanya.tradeflow.order.domain.Order;
import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderStatus;
import com.chaitanya.tradeflow.order.repo.OrderRepository;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

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
        CreateOrderRequest request = new CreateOrderRequest(123L, "INFY", 10, new BigDecimal("1520.50"), OrderSide.BUY);

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertEquals(OrderStatus.PENDING_RISK, response.status());
        assertEquals("INFY", response.symbol());
    }
}
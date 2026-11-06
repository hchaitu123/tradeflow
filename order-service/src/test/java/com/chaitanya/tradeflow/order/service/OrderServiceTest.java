package com.chaitanya.tradeflow.order.service;

import com.chaitanya.tradeflow.order.repo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
package com.chaitanya.tradeflow.order.domain;

import com.chaitanya.tradeflow.order.repo.OrderRepository;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class OrderOptimisticLockTest {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void secondStaleSave_throwsOptimisticLock(){

        Order order = new Order(123L);
        order.addItem(new OrderItem("INFY", 10, new BigDecimal("1520.50"),
                OrderSide.BUY));
        Long id = orderRepository.save(order).getOrderId();

        Order copyA = orderRepository.findById(id).orElseThrow();
        Order copyB = orderRepository.findById(id).orElseThrow();

        copyA.approve();
        orderRepository.save(copyA);

        copyB.reject("limit exceeded");
        assertThrows(ObjectOptimisticLockingFailureException.class,() -> orderRepository.save(copyB));
    }
}

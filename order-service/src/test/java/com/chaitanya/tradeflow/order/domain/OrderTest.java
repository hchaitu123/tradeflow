package com.chaitanya.tradeflow.order.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    @Test
    void approve_twice_throws() {
        Order order = new Order(123L);
        order.approve();

        assertThrows(IllegalStateException.class, order::approve);
    }

    @Test
    void reject_afterApprove_throws_andKeepsApproved() {
        Order order = new Order(123L);
        order.approve();

        assertThrows(IllegalStateException.class, () -> order.reject("late decision"));
        assertEquals(OrderStatus.APPROVED, order.getStatus());   // unchanged
    }
}
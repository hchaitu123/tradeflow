package com.chaitanya.tradeflow.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    private Long accountId;
    private String symbol;
    private Integer quantity;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String rejectionReason;
    private Instant createdAt;

    protected Order() {
    }
}

package com.chaitanya.tradeflow.order.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    private Long accountId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String rejectionReason;
    private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order(Long accountId) {
        this.accountId = accountId;
        this.createdAt = Instant.now();
        this.status = OrderStatus.PENDING_RISK;
    }

    protected Order() {
    }

    public void addItem(OrderItem item){
        item.setOrder(this);
        items.add(item);
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}

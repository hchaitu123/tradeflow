package com.chaitanya.tradeflow.order.web.dto;

import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(Long orderId, Long accountId, String symbol, Integer quantity,
                            BigDecimal price, OrderSide side, OrderStatus status,
                            String rejectionReason, Instant createdAt) {
}

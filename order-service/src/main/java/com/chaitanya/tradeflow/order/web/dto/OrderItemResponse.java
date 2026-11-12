package com.chaitanya.tradeflow.order.web.dto;

import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(Long orderItemId, String symbol, Integer quantity,
                                BigDecimal price, OrderSide side) {
    public static OrderItemResponse create(OrderItem item){
        return new OrderItemResponse(item.getOrderItemId(), item.getSymbol(), item.getQuantity(),
                item.getPrice(), item.getSide());
    }
}

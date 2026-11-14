package com.chaitanya.tradeflow.order.event;

import com.chaitanya.tradeflow.order.domain.OrderItem;
import com.chaitanya.tradeflow.order.domain.OrderSide;

import java.math.BigDecimal;

public record OrderCreatedEventItem(
    String symbol,
    Integer quantity,
    BigDecimal price,
    OrderSide side
            ){

    public static OrderCreatedEventItem from(OrderItem item){
        return new OrderCreatedEventItem(
                item.getSymbol(),
                item.getQuantity(),
                item.getPrice(),
                item.getSide()
        );
    }
}

package com.chaitanya.tradeflow.order.web.dto;

import com.chaitanya.tradeflow.order.domain.OrderSide;

import java.math.BigDecimal;

public record CreateOrderRequest(Long accountId, String symbol, Integer quantity, BigDecimal price, OrderSide side) {

}

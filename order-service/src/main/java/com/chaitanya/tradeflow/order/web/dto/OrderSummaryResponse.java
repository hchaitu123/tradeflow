package com.chaitanya.tradeflow.order.web.dto;

import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public record OrderSummaryResponse(Long accountId, Long totalOrders,
                                   Map<OrderStatus,Long> ordersByStatus, Map<String, BigDecimal> notionalBySymbol,
                                   Map<OrderSide,Long> itemsBySide, Map<Boolean, Long> largeItems) {
}

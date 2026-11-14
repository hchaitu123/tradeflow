package com.chaitanya.tradeflow.risk.event;

import java.math.BigDecimal;

public record OrderCreatedEventItem(
        String symbol,
        Integer quantity,
        BigDecimal price,
        String side
) {
}

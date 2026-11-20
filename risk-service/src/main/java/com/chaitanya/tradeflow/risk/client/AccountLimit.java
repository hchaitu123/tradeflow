package com.chaitanya.tradeflow.risk.client;

import java.math.BigDecimal;

public record AccountLimit(Long accountId, BigDecimal maxOrderValue) {}
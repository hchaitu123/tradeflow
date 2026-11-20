package com.chaitanya.tradeflow.account.web;

import java.math.BigDecimal;

public record AccountLimitResponse(Long accountId, BigDecimal maxOrderValue) {}

package com.chaitanya.tradeflow.order.web.dto;

import com.chaitanya.tradeflow.order.domain.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(@NotBlank String symbol, @NotNull @Positive Integer quantity,@NotNull @Positive BigDecimal price,@NotNull OrderSide side) {

}

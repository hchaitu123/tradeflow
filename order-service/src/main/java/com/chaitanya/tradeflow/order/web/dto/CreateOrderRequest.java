package com.chaitanya.tradeflow.order.web.dto;

import java.util.List;

public record CreateOrderRequest(Long accountId, List<OrderItemRequest> items) {

}

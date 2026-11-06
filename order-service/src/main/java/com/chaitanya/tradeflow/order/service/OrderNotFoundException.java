package com.chaitanya.tradeflow.order.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId){
        super("Order:" + orderId + " is not found.");
    }
}

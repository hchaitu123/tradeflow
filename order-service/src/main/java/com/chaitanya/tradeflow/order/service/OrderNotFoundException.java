package com.chaitanya.tradeflow.order.service;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId){
        super("Order:" + orderId + " is not found.");
    }
}

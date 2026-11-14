package com.chaitanya.tradeflow.order.listener;

import com.chaitanya.tradeflow.order.event.RiskDecidedEvent;
import com.chaitanya.tradeflow.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RiskDecidedListener {
    private static final Logger log = LoggerFactory.getLogger(RiskDecidedListener.class);
    private final OrderService orderService;

    public RiskDecidedListener(OrderService orderService){
        this.orderService = orderService;
    }

    @KafkaListener(topics = "orders.risk-decided")
    public void onRiskDecided(RiskDecidedEvent event){
        log.info("Order {} → {} {}", event.orderId(),
                event.approved() ? "APPROVED" : "REJECTED",
                event.reason() == null ? "" : "(" + event.reason() + ")");
        orderService.applyRiskDecision(event.orderId(), event.approved(), event.reason());
    }
}

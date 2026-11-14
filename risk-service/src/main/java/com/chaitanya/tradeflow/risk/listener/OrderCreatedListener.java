package com.chaitanya.tradeflow.risk.listener;

import com.chaitanya.tradeflow.risk.event.OrderCreatedEvent;
import com.chaitanya.tradeflow.risk.event.RiskDecidedEvent;
import com.chaitanya.tradeflow.risk.rules.RiskDecision;
import com.chaitanya.tradeflow.risk.rules.RiskRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
    private static final String RISK_DECIDED_TOPIC = "orders.risk-decided";

    private final RiskRules riskRules;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderCreatedListener(RiskRules riskRules, KafkaTemplate<String, Object> kafkaTemplate) {
        this.riskRules = riskRules;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "orders.created")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order {} for account {} with {} items",
                event.orderId(), event.accountId(), event.items().size());
        RiskDecision decision = riskRules.evaluate(event);
        kafkaTemplate.send(RISK_DECIDED_TOPIC, String.valueOf(event.orderId()),
                RiskDecidedEvent.from(event.orderId(), decision));
        log.info("Order {} → {} {}", event.orderId(),
                decision.approved() ? "APPROVED" : "REJECTED",
                decision.reason() == null ? "" : "(" + decision.reason() + ")");
    }
}

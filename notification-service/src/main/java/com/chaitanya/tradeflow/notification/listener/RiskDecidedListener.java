package com.chaitanya.tradeflow.notification.listener;

import com.chaitanya.tradeflow.notification.event.RiskDecidedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RiskDecidedListener {

    private static final Logger log = LoggerFactory.getLogger(RiskDecidedListener.class);

    @KafkaListener(topics = "orders.risk-decided")
    public void onRiskDecided(RiskDecidedEvent event) {
        if (event.approved()) {
            log.info("📩 Notify: order {} APPROVED", event.orderId());
        } else {
            log.info("📩 Notify: order {} REJECTED, reason: {}", event.orderId(), event.reason());
        }
    }
}

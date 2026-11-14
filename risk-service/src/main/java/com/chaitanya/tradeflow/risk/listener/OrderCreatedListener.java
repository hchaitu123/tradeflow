package com.chaitanya.tradeflow.risk.listener;

import com.chaitanya.tradeflow.risk.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Controller;

@Controller
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    @KafkaListener(topics = "orders.created")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order {} for account {} with {} items",
                event.orderId(), event.accountId(), event.items().size());
    }
}

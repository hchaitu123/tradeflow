package com.chaitanya.tradeflow.order;

import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderStatus;
import com.chaitanya.tradeflow.order.event.RiskDecidedEvent;
import com.chaitanya.tradeflow.order.repo.OrderRepository;
import com.chaitanya.tradeflow.order.service.OrderService;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderItemRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TestcontainersConfiguration.class)
public class RiskDecisionFlowTest {

    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void approvedDecision_fromKafka_approvesOrder() {
        // 1. create a real order in the real (container) Postgres
        OrderResponse created = orderService.createOrder(new CreateOrderRequest(123L,
                List.of(new OrderItemRequest("INFY", 10, new BigDecimal("1500.00"), OrderSide.BUY))));
        Long orderId = created.orderId();
        assertEquals(OrderStatus.PENDING_RISK, created.status());

        // 2. pretend to be risk-service: send an "approved" decision into Kafka
        RiskDecidedEvent decision = new RiskDecidedEvent(UUID.randomUUID(), Instant.now(), orderId, true, null);
        kafkaTemplate.send("orders.risk-decided", String.valueOf(orderId), decision);

        // 3. wait (up to 15 s) until the listener has applied it
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertEquals(OrderStatus.APPROVED,
                        orderRepository.findById(orderId).orElseThrow().getStatus()));
    }
}

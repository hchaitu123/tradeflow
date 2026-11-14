package com.chaitanya.tradeflow.risk.rules;

import com.chaitanya.tradeflow.risk.event.OrderCreatedEvent;
import com.chaitanya.tradeflow.risk.event.OrderCreatedEventItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RiskRulesTest {

    private final RiskRules rules = new RiskRules();

    private OrderCreatedEvent eventWith(OrderCreatedEventItem... items){
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(),1L, 123L, List.of(items));
    }

    private OrderCreatedEventItem item(String symbol, int quantity, String price){
        return new OrderCreatedEventItem(symbol, quantity, new BigDecimal(price), "BUY");
    }

    @Test
    void approves_validOrder() {
        RiskDecision d = rules.evaluate(eventWith(item("INFY", 10, "1500.00")));
        assertTrue(d.approved());
    }

    @Test
    void rejects_unknownSymbol() {
        RiskDecision d = rules.evaluate(eventWith(item("XYZ", 10, "100.00")));
        assertFalse(d.approved());
        assertTrue(d.reason().contains("is not allowed"));
    }

    @Test
    void rejects_quantityAboveMax() {
        RiskDecision d = rules.evaluate(eventWith(item("TCS", RiskRules.MAX_QUANTITY + 1, "10.00")));
        assertFalse(d.approved());
        assertTrue(d.reason().contains("out of range"));     // after line 42
    }

    @Test
    void rejects_orderValueAboveLimit() {
        // 2 items, each fine alone, but together > ₹10,00,000
        RiskDecision d = rules.evaluate(eventWith(
                item("RELIANCE", 200, "2500.00"),     // 5,00,000
                item("INFY", 400, "1500.00")));   // make the total go over 10,00,000
        assertFalse(d.approved());
        assertTrue(d.reason().contains("exceeds limit"));    // after line 51
    }

}
package com.chaitanya.tradeflow.risk.rules;

import com.chaitanya.tradeflow.risk.event.OrderCreatedEvent;
import com.chaitanya.tradeflow.risk.event.OrderCreatedEventItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class RiskRules {
    static final Set<String> ALLOWED_SYMBOLS = Set.of("INFY", "TCS", "RELIANCE", "HDFCBANK", "ITC");
    static final int MAX_QUANTITY = 10_000;
    static final BigDecimal MAX_ORDER_NOTIONAL = new BigDecimal("1000000");

    public RiskDecision evaluate(OrderCreatedEvent event){
        for(OrderCreatedEventItem item : event.items()){
            if(!ALLOWED_SYMBOLS.contains(item.symbol())) {
                return RiskDecision.reject("Symbol " + item.symbol() + " is not allowed");
            }
            if(item.quantity() > MAX_QUANTITY || item.quantity() < 1) {
                return RiskDecision.reject("Quantity " + item.quantity() + " out of range for " + item.symbol());
            }
        }
        BigDecimal eventNotional = event.items().stream().
                map(ele -> ele.price().multiply(BigDecimal.valueOf(ele.quantity()))).
                reduce(BigDecimal.ZERO,BigDecimal::add);
        if(eventNotional.compareTo(MAX_ORDER_NOTIONAL)>0) {
            return RiskDecision.reject("Order value " + eventNotional + " exceeds limit " + MAX_ORDER_NOTIONAL);
        }
        return RiskDecision.approve();
    }
}

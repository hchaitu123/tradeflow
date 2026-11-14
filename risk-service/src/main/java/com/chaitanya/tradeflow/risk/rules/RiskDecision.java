package com.chaitanya.tradeflow.risk.rules;

public record RiskDecision(boolean approved, String reason) {

    public static RiskDecision approve() {
        return new RiskDecision(true, null);
    }

    public static RiskDecision reject(String reason) {
        return new RiskDecision(false, reason);
    }
}

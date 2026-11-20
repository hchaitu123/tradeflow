package com.chaitanya.tradeflow.risk.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class AccountClient {

    private final RestClient restClient;
    private static final Logger log = LoggerFactory.getLogger(AccountClient.class);

    public AccountClient(RestClient.Builder builder,
                         @Value("${account-service.url}") String baseUrl) {         // ③ the yaml key from B2
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "account", fallbackMethod = "limitFallback")
    public BigDecimal getMaxOrderValue(Long accountId) {
        AccountLimit limit = restClient.get()                       // ④ which HTTP method?
                .uri("/accounts/{id}/limit", accountId)
                .retrieve()                                          // send it; 4xx/5xx → exception
                .body(AccountLimit.class);                                   // ⑤ turn the JSON into which class?
        return limit.maxOrderValue();
    }

    // fallback: SAME return type, SAME parameters + one extra Throwable at the end
    private BigDecimal limitFallback(Long accountId, Throwable ex) {
        log.warn("⚡ account-service unavailable for account {} → limit 0 ({})",
                accountId, ex.getClass().getSimpleName());
        return BigDecimal.ZERO;                                   // ④ "no money allowed"
    }
}
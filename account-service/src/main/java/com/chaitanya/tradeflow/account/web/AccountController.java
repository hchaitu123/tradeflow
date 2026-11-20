package com.chaitanya.tradeflow.account.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    // fake data for now (no DB): account 1 = rich, account 2 = small
    private static final Map<Long, BigDecimal> LIMITS = Map.of(
            1L, new BigDecimal("500000"),
            2L, new BigDecimal("50000"));

    @GetMapping("/{id}/limit")
    public AccountLimitResponse getLimit(@PathVariable Long id) {          // ④ value comes from the URL path
        BigDecimal limit = LIMITS.get(id);
        if (limit == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,       // ⑤ which status = "doesn't exist"?
                    "Account " + id + " not found");
        }
        return new AccountLimitResponse(id, limit);                                  // ⑥
    }
}
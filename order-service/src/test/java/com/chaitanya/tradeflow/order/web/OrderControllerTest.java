package com.chaitanya.tradeflow.order.web;

import com.chaitanya.tradeflow.order.domain.OrderSide;
import com.chaitanya.tradeflow.order.domain.OrderStatus;
import com.chaitanya.tradeflow.order.service.OrderNotFoundException;
import com.chaitanya.tradeflow.order.service.OrderService;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @Test
    void getOrder_whenMissing_returns404() throws Exception {
        when(orderService.findById(5L)).thenThrow(new OrderNotFoundException(5L));
        mockMvc.perform(get("/orders/5")).andExpect(status().isNotFound());
    }

    @Test
    void createOrder_returns201WithBody() throws Exception {
        // Arrange: what the fake service returns
        OrderResponse response = new OrderResponse(1L, 123L, "INFY", 10,
                new BigDecimal("1520.50"), OrderSide.BUY, OrderStatus.PENDING_RISK,
                null, Instant.now());
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        // the request body, as raw JSON text (a Java "text block": three quotes)
        String json = """
            {
              "accountId": 123,
              "symbol": "INFY",
              "quantity": 10,
              "price": 1520.50,
              "side": "BUY"
            }
            """;

        // Act + Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)   // tell Spring "this body is JSON"
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING_RISK"));
    }
}
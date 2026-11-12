package com.chaitanya.tradeflow.order.web;

import com.chaitanya.tradeflow.order.service.OrderService;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import com.chaitanya.tradeflow.order.web.dto.OrderSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id){
        OrderResponse order = orderService.findById(id);
        return new ResponseEntity<>(order,HttpStatusCode.valueOf(200) );
    }

    @GetMapping
    public PagedModel<OrderResponse> listOrders(@RequestParam Long accountId, Pageable pageable) {
        return new PagedModel<>(orderService.findByAccountId(accountId, pageable));
    }

    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest orderRequest){
        OrderResponse order = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(order,HttpStatusCode.valueOf(201) );
    }

    @GetMapping("/summary")
    public OrderSummaryResponse summary(@RequestParam Long accountId) {
        return orderService.summary(accountId);
    }
}

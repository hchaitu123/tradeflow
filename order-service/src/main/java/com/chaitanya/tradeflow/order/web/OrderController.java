package com.chaitanya.tradeflow.order.web;

import com.chaitanya.tradeflow.order.service.OrderService;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import jakarta.validation.Valid;
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

    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest orderRequest){
        OrderResponse order = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(order,HttpStatusCode.valueOf(201) );
    }
}

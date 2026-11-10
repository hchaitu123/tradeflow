package com.chaitanya.tradeflow.order.service;

import com.chaitanya.tradeflow.order.domain.Order;
import com.chaitanya.tradeflow.order.domain.OrderItem;
import com.chaitanya.tradeflow.order.repo.OrderRepository;
import com.chaitanya.tradeflow.order.web.dto.CreateOrderRequest;
import com.chaitanya.tradeflow.order.web.dto.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::create)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest orderRequest) {
        Order saved = orderRepository.save(createOrderFromOrderRequest(orderRequest));
        return OrderResponse.create(saved);
    }

    private Order createOrderFromOrderRequest(CreateOrderRequest orderRequest) {
       Order order = new Order(orderRequest.accountId());
       orderRequest.items().forEach(i -> order.addItem(new OrderItem(i.symbol(), i.quantity(), i.price(), i.side())));
        return order;
    }


}

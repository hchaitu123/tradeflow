package com.chaitanya.tradeflow.order.repo;

import com.chaitanya.tradeflow.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

}

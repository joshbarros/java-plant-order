package com.plantorder.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLineEntity, String> {
    List<OrderLineEntity> findByOrderIdAndState(String orderId, String state);
    List<OrderLineEntity> findByOrderIdOrderByPositionAsc(String orderId);
}

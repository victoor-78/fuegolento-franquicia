package com.fuegolento.fuegolento_franquicia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    Optional<OrderItem> findByOrderAndDish_Id(Order order, Long dishId);

    /**
     * Badge counter option A (recommended):
     * - counts total quantity (e.g., 2 croquetas + 1 agua = 3)
     */
    @Query("select coalesce(sum(oi.quantity), 0) from OrderItemTable oi where oi.order = :order")
    int sumQuantitiesByOrder(@Param("order") Order order);

    /**
     * Badge counter option B (optional):
     * - counts distinct lines/items (e.g., croquetas + agua = 2)
     */
    long countByOrder(Order order);
}
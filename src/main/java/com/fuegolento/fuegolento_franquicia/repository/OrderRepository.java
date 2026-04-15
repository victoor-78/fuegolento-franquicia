package com.fuegolento.fuegolento_franquicia.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.User;

/**
 * Repository for Order entity.
 *
 * Cart strategy:
 * - The current cart is the most recent Order with status = PENDING.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders of a user
    List<Order> findByUser(User user);

    // Get the latest order of a user with a given status (used for cart=PENDING)
    Optional<Order> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, OrderStatus status);

    // Get all orders with a specific status
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    // Get all orders of a user with a specific status
    List<Order> findByUserAndStatus(User user, OrderStatus status);

    // Get orders created between two dates (for statistics)
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Get completed orders (DELIVERED) between dates
    List<Order> findByStatusAndCreatedAtBetween(
        OrderStatus status,
        LocalDateTime start,
        LocalDateTime end
    );

    List<Order> findByUserAndStatusNotOrderByCreatedAtDesc(User user, OrderStatus status);
    // ========= DASHBOARD QUERIES =========

    interface DailyRevenueRow {
        java.sql.Date getDay();
        java.math.BigDecimal getRevenue();
    }

    interface HourCountRow {
        Integer getHour();
        Long getCount();
    }

    @Query("""
        SELECT
            function('date', o.createdAt) as day,
            sum(o.totalPrice) as revenue
        FROM OrderTable o
        WHERE o.status = com.fuegolento.fuegolento_franquicia.enums.OrderStatus.DELIVERED
          AND o.totalPrice IS NOT NULL
          AND o.createdAt >= :from
        GROUP BY function('date', o.createdAt)
        ORDER BY function('date', o.createdAt)
    """)
    List<DailyRevenueRow> revenueByDaySince(@Param("from") LocalDateTime from);

    @Query("""
        SELECT
            function('hour', o.createdAt) as hour,
            count(o) as count
        FROM OrderTable o
        WHERE o.status = com.fuegolento.fuegolento_franquicia.enums.OrderStatus.DELIVERED
          AND o.createdAt >= :from
        GROUP BY function('hour', o.createdAt)
        ORDER BY function('hour', o.createdAt)
    """)
    List<HourCountRow> deliveredCountByHourSince(@Param("from") LocalDateTime from);
}
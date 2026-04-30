package com.fuegolento.fuegolento_franquicia.service;

import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class KitchenService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public KitchenService(OrderRepository orderRepository,
                          OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /* =========================
       KDS COLUMNS
       ========================= */

    public List<Order> getReceivedOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.SENT_TO_KITCHEN);
    }

    public List<Order> getInProgressOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.IN_PROGRESS);
    }

    public List<Order> getReadyOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.READY);
    }

    public KitchenBoard getBoard() {
        KitchenBoard board = new KitchenBoard();
        board.received = getReceivedOrders();
        board.inProgress = getInProgressOrders();
        board.ready = getReadyOrders();
        return board;
    }

    public KitchenBoard getBoardForRestaurant(Restaurant restaurant) {
        KitchenBoard board = new KitchenBoard();
        board.received = orderRepository.findByRestaurantAndStatusOrderByCreatedAtAsc(restaurant, OrderStatus.SENT_TO_KITCHEN);
        board.inProgress = orderRepository.findByRestaurantAndStatusOrderByCreatedAtAsc(restaurant, OrderStatus.IN_PROGRESS);
        board.ready = orderRepository.findByRestaurantAndStatusOrderByCreatedAtAsc(restaurant, OrderStatus.READY);
        return board;
    }

    /* =========================
       ACTIONS (status transitions)
       ========================= */

    public Order moveToInProgress(Long orderId) {
        return orderService.startPreparing(orderId);
    }

    public Order moveToReady(Long orderId) {
        return orderService.markReady(orderId);
    }

    /**
     * READY -> DELIVERED (recogida por camarero / entregada)
     */
    public Order moveToDelivered(Long orderId) {
        return orderService.deliver(orderId);
    }

    public Order backToReceived(Long orderId) {
        Order order = orderService.findById(orderId);

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS orders can go back to SENT_TO_KITCHEN");
        }

        order.setStatus(OrderStatus.SENT_TO_KITCHEN);
        return orderRepository.save(order);
    }

    public Order backToInProgress(Long orderId) {
        Order order = orderService.findById(orderId);

        if (order.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException("Only READY orders can go back to IN_PROGRESS");
        }

        order.setStatus(OrderStatus.IN_PROGRESS);
        return orderRepository.save(order);
    }

    public static class KitchenBoard {
        public List<Order> received;
        public List<Order> inProgress;
        public List<Order> ready;
    }
}
package com.fuegolento.fuegolento_franquicia.service;

import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.OrderItemRepository;
import com.fuegolento.fuegolento_franquicia.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishService dishService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        DishService dishService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.dishService = dishService;
    }

    /* =========================
       READ
       ========================= */

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    public List<Order> findByUser(User user) {
        return orderRepository.findByUser(user);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    public List<Order> findByUserAndStatus(User user, OrderStatus status) {
        return orderRepository.findByUserAndStatus(user, status);
    }

    public List<Order> findDeliveredBetween(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByStatusAndCreatedAtBetween(OrderStatus.DELIVERED, start, end);
    }

    /* =========================
       CART (PENDING order in DB)
       ========================= */

    /**
     * Returns the restaurant-scoped cart of the user (latest PENDING order for that restaurant).
     * If not exists, creates a new PENDING order bound to the restaurant.
     */
    public Order getOrCreateCart(User user, Restaurant restaurant) {
        if (user == null) throw new IllegalArgumentException("User is required");
        if (restaurant == null) throw new IllegalArgumentException("Restaurant is required");

        return orderRepository
                .findFirstByUserAndRestaurantAndStatusOrderByCreatedAtDesc(user, restaurant, OrderStatus.PENDING)
                .orElseGet(() -> {
                    Order order = new Order(user);
                    order.setRestaurant(restaurant);
                    order.setStatus(OrderStatus.PENDING);
                    return orderRepository.save(order);
                });
    }

    /**
     * Returns the restaurant-scoped cart if it exists, otherwise null.
     * When restaurant is null, falls back to the most recent PENDING cart regardless of restaurant
     * (used for read-only views like the cart badge).
     */
    public Order getCartIfExists(User user, Restaurant restaurant) {
        if (user == null) throw new IllegalArgumentException("User is required");

        if (restaurant != null) {
            return orderRepository
                    .findFirstByUserAndRestaurantAndStatusOrderByCreatedAtDesc(user, restaurant, OrderStatus.PENDING)
                    .orElse(null);
        }
        return orderRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, OrderStatus.PENDING)
                .orElse(null);
    }

    /**
     * Restaurant-agnostic overload — used by GlobalModelAttributes (cart badge)
     * and any context where a restaurant is not yet selected.
     */
    public Order getCartIfExists(User user) {
        return getCartIfExists(user, null);
    }

    /**
     * Adds quantity units of a dish to the user's restaurant-scoped cart.
     */
    public Order addToCart(User user, Restaurant restaurant, Long dishId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");

        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        Dish dish = dishService.findById(dishId);
        if (!dish.isAvailable()) {
            throw new IllegalStateException("Dish is not available");
        }

        OrderItem item = orderItemRepository.findByOrderAndDish_Id(cart, dishId).orElse(null);

        if (item == null) {
            OrderItem newItem = new OrderItem(dish, quantity, dish.getPrice());
            cart.addItem(newItem);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }

        return orderRepository.save(cart);
    }

    /**
     * Sets quantity for a dish inside the user's restaurant-scoped cart.
     * If quantity <= 0, removes the item.
     */
    public Order setCartDishQuantity(User user, Restaurant restaurant, Long dishId, int quantity) {
        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        OrderItem item = orderItemRepository.findByOrderAndDish_Id(cart, dishId)
                .orElseThrow(() -> new RuntimeException("Dish not found in cart"));

        if (quantity <= 0) {
            cart.removeItem(item);
        } else {
            item.setQuantity(quantity);
        }

        return orderRepository.save(cart);
    }

    /**
     * Removes a dish from the user's restaurant-scoped cart.
     */
    public Order removeFromCart(User user, Restaurant restaurant, Long dishId) {
        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        OrderItem item = orderItemRepository.findByOrderAndDish_Id(cart, dishId)
                .orElseThrow(() -> new RuntimeException("Dish not found in cart"));

        cart.removeItem(item);
        return orderRepository.save(cart);
    }

    /**
     * Empties the user's restaurant-scoped cart (removes all items).
     */
    public Order clearCart(User user, Restaurant restaurant) {
        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        if (cart.getItems() != null) {
            cart.getItems().clear();
        }

        return orderRepository.save(cart);
    }

    /**
     * Cart badge count:
     * total quantities (e.g., 2+1 = 3)
     */
    public int getCartItemCount(User user) {
        Order cart = getCartIfExists(user);
        if (cart == null) return 0;

        return orderItemRepository.sumQuantitiesByOrder(cart);
    }

    public BigDecimal calculateCartTotal(Order cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }
        return cart.calculateTotalFromItems();
    }

    /* =========================
       SUBMIT / STATUS CHANGES
       ========================= */

    /**
     * Submits the restaurant-scoped cart to the kitchen.
     * PENDING -> SENT_TO_KITCHEN
     */
    public Order submitCart(User user, Restaurant restaurant) {
        Order cart = getCartIfExists(user, restaurant);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot submit an empty cart");
        }
        ensureEditable(cart);

        cart.setStatus(OrderStatus.SENT_TO_KITCHEN);
        return orderRepository.save(cart);
    }

    /**
     * SENT_TO_KITCHEN -> IN_PROGRESS
     */
    public Order startPreparing(Long orderId) {
        Order order = findById(orderId);

        if (order.getStatus() != OrderStatus.SENT_TO_KITCHEN) {
            throw new IllegalStateException("Only SENT_TO_KITCHEN orders can be started");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot start an empty order");
        }

        order.setStatus(OrderStatus.IN_PROGRESS);
        return orderRepository.save(order);
    }

    /**
     * IN_PROGRESS -> READY
     */
    public Order markReady(Long orderId) {
        Order order = findById(orderId);

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS orders can be marked as READY");
        }

        order.setStatus(OrderStatus.READY);
        return orderRepository.save(order);
    }

    /**
     * READY -> DELIVERED and stores total snapshot.
     */
    public Order deliver(Long orderId) {
        Order order = findById(orderId);

        if (order.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException("Only READY orders can be delivered");
        }

        BigDecimal total = order.calculateTotalFromItems();

        // Snapshot for auditing/history
        order.setTotalPriceSnapshot(total);

        // Optional: if your entity also has totalPrice used by views
        // (Remove this line if your Order entity does not have setTotalPrice)
        // order.setTotalPrice(total);

        order.setStatus(OrderStatus.DELIVERED);

        return orderRepository.save(order);
    }

    /**
     * Cancels an order if it is not already DELIVERED.
     */
    public Order cancel(Long orderId) {
        Order order = findById(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Delivered orders cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public Order setCartItemKitchenNote(User user, Restaurant restaurant, Long dishId, String kitchenNote) {
        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        OrderItem item = orderItemRepository.findByOrderAndDish_Id(cart, dishId)
                .orElseThrow(() -> new RuntimeException("Dish not found in cart"));

        if (kitchenNote != null && kitchenNote.trim().isEmpty()) kitchenNote = null;

        item.setKitchenNote(kitchenNote);
        return orderRepository.save(cart);
    }

    public Order setCartItemMeatPoint(User user, Restaurant restaurant, Long dishId, String meatPoint) {
        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        Dish dish = dishService.findById(dishId);
        if (dish.getCategory() != DishCategory.MEAT) {
            throw new IllegalStateException("Meat point can only be set for MEAT dishes");
        }

        if (meatPoint != null) {
            meatPoint = meatPoint.trim().toUpperCase();
        }

        if (meatPoint == null || meatPoint.isEmpty()) {
            meatPoint = null;
        } else {
            switch (meatPoint) {
                case "MUY_HECHO":
                case "HECHO":
                case "AL_PUNTO":
                case "POCO_HECHO":
                    break;
                default:
                    throw new IllegalArgumentException("Invalid meat point");
            }
        }

        OrderItem item = orderItemRepository.findByOrderAndDish_Id(cart, dishId)
                .orElseThrow(() -> new RuntimeException("Dish not found in cart"));

        item.setMeatPoint(meatPoint);

        return orderRepository.save(cart);
    }

    public Order setCartTableNumber(User user, Restaurant restaurant, Integer tableNumber) {
        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        if (tableNumber == null || tableNumber < 1 || tableNumber > 20) {
            throw new IllegalArgumentException("Table number must be between 1 and 20");
        }

        cart.setTableNumber(tableNumber);
        return orderRepository.save(cart);
    }

    public Order setCartCustomerNote(User user, Restaurant restaurant, String customerNote) {
        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        if (customerNote != null && customerNote.trim().isEmpty()) customerNote = null;

        cart.setCustomerNote(customerNote);
        return orderRepository.save(cart);
    }

    /* =========================
       DELETE (optional admin)
       ========================= */

    public void deleteById(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new RuntimeException("Order not found with id: " + orderId);
        }
        orderRepository.deleteById(orderId);
    }

    /**
     * User "orders history": excludes the cart (PENDING).
     */
    public List<Order> getOrderHistory(User user) {
        return orderRepository.findByUserAndStatusNotOrderByCreatedAtDesc(user, OrderStatus.PENDING);
    }

    /* =========================
       PRIVATE
       ========================= */

    private void ensureEditable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be modified");
        }
    }

    //ADMIN ORDER STATUS CHANGE (from details page, no status flow checks)
    public Order adminMoveStatus(Long orderId, OrderStatus target) {

        Order order = findById(orderId);
        OrderStatus current = order.getStatus();

        if (target == current) return order;

        switch (target) {

            case SENT_TO_KITCHEN -> {
                // Only makes sense if it was a cart (PENDING)
                if (current != OrderStatus.PENDING) {
                    throw new IllegalStateException("Only PENDING orders can be sent to kitchen");
                }
                order.setStatus(OrderStatus.SENT_TO_KITCHEN);
                return orderRepository.save(order);
            }

            case IN_PROGRESS -> {
                // If it was SENT_TO_KITCHEN -> startPreparing
                if (current == OrderStatus.SENT_TO_KITCHEN) {
                    return startPreparing(orderId);
                }
                // If admin forces it:
                order.setStatus(OrderStatus.IN_PROGRESS);
                return orderRepository.save(order);
            }

            case READY -> {
                if (current == OrderStatus.IN_PROGRESS) {
                    return markReady(orderId);
                }
                // If it was SENT_TO_KITCHEN -> go IN_PROGRESS then READY
                if (current == OrderStatus.SENT_TO_KITCHEN) {
                    startPreparing(orderId);
                    return markReady(orderId);
                }
                // If admin forces it:
                order.setStatus(OrderStatus.READY);
                return orderRepository.save(order);
            }

            case DELIVERED -> {
                // Make it consistent: ensure READY before deliver
                if (current == OrderStatus.SENT_TO_KITCHEN) {
                    startPreparing(orderId);
                    markReady(orderId);
                    return deliver(orderId);
                }
                if (current == OrderStatus.IN_PROGRESS) {
                    markReady(orderId);
                    return deliver(orderId);
                }
                if (current == OrderStatus.READY) {
                    return deliver(orderId);
                }

                // If admin tries to deliver a cart or cancelled, block
                throw new IllegalStateException("Cannot deliver an order from status: " + current);
            }

            case CANCELLED -> {
                return cancel(orderId);
            }

            case PENDING -> {
                // Generally you don't want to revert to cart from admin
                throw new IllegalStateException("Cannot move an order back to PENDING (cart)");
            }
        }

        throw new IllegalStateException("Unsupported status change");
    }

    /* =========================
       DUPLICATE ORDER (Access Control Demo)
       ========================= */

    /**
     * ✅ Duplicates a previous order into the current cart.
     * Educational demo for access control: validates user ownership.
     * fallbackRestaurant is used when the source order has no restaurant (legacy data).
     */
    public Order duplicateOrder(User user, Long orderId, Restaurant fallbackRestaurant) {
        Order orderToDuplicate = findById(orderId);

        Restaurant restaurant = orderToDuplicate.getRestaurant();
        if (restaurant == null) restaurant = fallbackRestaurant;
        if (restaurant == null) throw new IllegalStateException("Source order has no restaurant");

        Order cart = getOrCreateCart(user, restaurant);
        ensureEditable(cart);

        // Copy all items from the old order to cart
        if (orderToDuplicate.getItems() != null) {
            for (OrderItem item : orderToDuplicate.getItems()) {
                OrderItem newItem = new OrderItem(
                        item.getDish(),
                        item.getQuantity(),
                        item.getDish().getPrice()
                );
                
                // Copy special notes if present
                if (item.getMeatPoint() != null) {
                    newItem.setMeatPoint(item.getMeatPoint());
                }
                if (item.getKitchenNote() != null) {
                    newItem.setKitchenNote(item.getKitchenNote());
                }
                
                cart.addItem(newItem);
            }
        }

        return orderRepository.save(cart);
    }
    
}
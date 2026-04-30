package com.fuegolento.fuegolento_franquicia.controller;

import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.RestaurantRepository;
import com.fuegolento.fuegolento_franquicia.service.OrderService;
import com.fuegolento.fuegolento_franquicia.service.UserService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserOrdersController {

    private final UserService userService;
    private final OrderService orderService;
    private final RestaurantRepository restaurantRepository;

    public UserOrdersController(UserService userService, OrderService orderService,
                                RestaurantRepository restaurantRepository) {
        this.userService = userService;
        this.orderService = orderService;
        this.restaurantRepository = restaurantRepository;
    }

    @GetMapping("/orders")
    public String ordersPage(Model model, CsrfToken csrfToken) {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // History without PENDING (cart)
        List<Order> orders = orderService.getOrderHistory(user);

        boolean hasOrders = orders != null && !orders.isEmpty();
        model.addAttribute("hasOrders", hasOrders);

        if (csrfToken != null) {
            model.addAttribute("token", csrfToken.getToken());
        }

        if (hasOrders) {
            model.addAttribute("orders", buildOrdersViewModel(orders));
        } else {
            model.addAttribute("orders", null);
        }

        return "orders"; // templates/orders.html
    }

    /**
     * ✅ CONTROL DE ACCESO: Duplicar/Reordenar una orden antigua
     * Solo permite duplicar si la orden pertenece al usuario autenticado
     * GET endpoint para fácil demostración (visible en URL)
     */
    @GetMapping("/orders/duplicate")
    public void duplicateOrder(@RequestParam("orderId") Long orderId,
                               HttpServletResponse response,
                               HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Order orderToDuplicate = orderService.findById(orderId);

        // ✅ VALIDACIÓN DE PERTENENCIA (CONTROL DE ACCESO)
        if (orderToDuplicate == null || !orderToDuplicate.getUser().getId().equals(user.getId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendError(403, "No tienes permiso para duplicar esta orden");
            return;
        }

        // Resolve fallback restaurant from session (used when source order has no restaurant)
        Restaurant fallbackRestaurant = null;
        if (orderToDuplicate.getRestaurant() == null) {
            Long restaurantId = (Long) session.getAttribute("restaurantId");
            if (restaurantId == null) {
                response.sendRedirect("/restaurants");
                return;
            }
            fallbackRestaurant = restaurantRepository.findById(restaurantId).orElse(null);
            if (fallbackRestaurant == null) {
                response.sendRedirect("/restaurants");
                return;
            }
        }

        orderService.duplicateOrder(user, orderId, fallbackRestaurant);
        response.sendRedirect("/order");
    }

    private List<Map<String, Object>> buildOrdersViewModel(List<Order> orders) {

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");

        for (Order o : orders) {
            Map<String, Object> vm = new HashMap<>();

            vm.put("id", o.getId());

            String restaurantName = (o.getRestaurant() != null) ? o.getRestaurant().getName() : "";
            vm.put("restaurantName", restaurantName);
            vm.put("hasRestaurantName", !restaurantName.isEmpty());

            String createdAtText = (o.getCreatedAt() != null) ? o.getCreatedAt().format(fmt) : "";
            vm.put("createdAtText", createdAtText);

            vm.put("tableNumber", o.getTableNumber()); // can be null

            OrderStatus st = o.getStatus();
            vm.put("statusText", statusText(st));
            vm.put("statusClass", statusClass(st));
            vm.put("statusIcon", statusIcon(st));

            String note = (o.getCustomerNote() == null || o.getCustomerNote().trim().isEmpty())
                    ? "—"
                    : o.getCustomerNote();
            vm.put("noteText", note);
            vm.put("noteEmpty", "—".equals(note));

            vm.put("isDelivered", st == OrderStatus.DELIVERED);
            vm.put("items", buildOrderItemsViewModel(o.getItems()));

            BigDecimal total;
            if (st == OrderStatus.DELIVERED && o.getTotalPrice() != null) {
                total = o.getTotalPrice(); // snapshot
            } else {
                total = (o.getItems() == null) ? BigDecimal.ZERO : o.calculateTotalFromItems();
            }
            vm.put("totalText", formatMoney(total));

            result.add(vm);
        }

        return result;
    }

    private List<Map<String, Object>> buildOrderItemsViewModel(List<OrderItem> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items == null) return result;

        for (OrderItem oi : items) {
            Dish d = oi.getDish();

            Map<String, Object> vm = new HashMap<>();
            vm.put("quantity", oi.getQuantity());
            vm.put("name", (d != null) ? d.getName() : "Producto");
            vm.put("lineTotalText", formatMoney(oi.getTotalPrice()));

            boolean isMeat = (d != null && d.getCategory() == DishCategory.MEAT);
            String mp = oi.getMeatPoint();

            if (isMeat) {
                if (mp == null || mp.isBlank()) mp = "HECHO";
                vm.put("meatPointText", meatPointText(mp));
            } else {
                vm.put("meatPointText", null);
            }

            String kitchenNote = oi.getKitchenNote();
            if (kitchenNote != null && kitchenNote.trim().isEmpty()) kitchenNote = null;
            vm.put("kitchenNote", kitchenNote);

            result.add(vm);
        }

        return result;
    }

    private String statusText(OrderStatus st) {
        if (st == null) return "—";
        return switch (st) {
            case SENT_TO_KITCHEN -> "Enviado a cocina";
            case IN_PROGRESS -> "En preparación";
            case READY -> "Listo";
            case DELIVERED -> "Servido";
            case CANCELLED -> "Cancelado";
            case PENDING -> "Carrito";
        };
    }

    private String statusClass(OrderStatus st) {
        if (st == null) return "status-sent";
        return switch (st) {
            case SENT_TO_KITCHEN -> "status-sent";
            case IN_PROGRESS -> "status-cooking";
            case READY -> "status-ready";
            case DELIVERED -> "status-served";
            case CANCELLED -> "status-cancelled";
            case PENDING -> "status-sent";
        };
    }

    private String statusIcon(OrderStatus st) {
        if (st == null) return "bi-info-circle";
        return switch (st) {
            case SENT_TO_KITCHEN -> "bi-send-check";
            case IN_PROGRESS -> "bi-hourglass-split";
            case READY -> "bi-bell";
            case DELIVERED -> "bi-check2-circle";
            case CANCELLED -> "bi-x-circle";
            case PENDING -> "bi-cart";
        };
    }

    private String meatPointText(String mp) {
        if (mp == null) return null;
        return switch (mp) {
            case "MUY_HECHO" -> "Muy hecho";
            case "HECHO" -> "Hecho";
            case "AL_PUNTO" -> "Al punto";
            case "POCO_HECHO" -> "Poco hecho";
            default -> mp;
        };
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
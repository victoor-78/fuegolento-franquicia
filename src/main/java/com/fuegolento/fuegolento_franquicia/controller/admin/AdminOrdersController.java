package com.fuegolento.fuegolento_franquicia.controller.admin;

import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrdersController {

    private final OrderService orderService;

    public AdminOrdersController(OrderService orderService) {
        this.orderService = orderService;
    }

    /* =========================
       LIST
       ========================= */

    @GetMapping
    public String list(
            @RequestParam(value = "q", required = false) String q,
            Model model,
            CsrfToken csrfToken
    ) {
        // Admin-only is already enforced by WebSecurityConfig: /admin/**

        List<Order> orders = orderService.findAll();

        // Usually you don't want to show the cart orders (PENDING) in admin management
        orders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.PENDING)
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        if (q != null && !q.isBlank()) {
            String query = q.trim().toLowerCase();
            orders = orders.stream()
                    .filter(o ->
                            ("#" + o.getId()).toLowerCase().contains(query) ||
                            (o.getTableNumber() != null && String.valueOf(o.getTableNumber()).contains(query))
                    )
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> vm = buildOrdersVmForTable(orders);

        model.addAttribute("orders", vm);
        model.addAttribute("hasOrders", !vm.isEmpty());
        model.addAttribute("searchQuery", (q == null ? "" : q));

        // Navbar active flags (for your header-admin)
        model.addAttribute("activeOrders", true);

        // CSRF
        if (csrfToken != null) {
            model.addAttribute("csrfToken", csrfToken.getToken());
        }

        return "admin-orders";
    }

    /* =========================
       DETAILS
       ========================= */

    @GetMapping("/{id}")
    public String details(@PathVariable("id") Long id, Model model, CsrfToken csrfToken) {

        Order order = orderService.findById(id);

        Map<String, Object> orderVm = buildSingleOrderVm(order);

        model.addAttribute("order", orderVm);
        model.addAttribute("activeOrders", true);

        if (csrfToken != null) {
            model.addAttribute("csrfToken", csrfToken.getToken());
        }

        return "admin-order-details";
    }

    /* =========================
       ACTION: SET STATUS
       ========================= */

    @PostMapping("/set-status")
    public void setStatus(
            @RequestParam("orderId") Long orderId,
            @RequestParam("status") String status,
            HttpServletResponse response
    ) throws IOException {

        OrderStatus newStatus = OrderStatus.valueOf(status);

        orderService.adminMoveStatus(orderId, newStatus);

        // clean redirect (no token in URL)
        response.sendRedirect("/admin/orders");
    }

    @PostMapping("/set-status-from-details")
    public void setStatusFromDetails(
            @RequestParam("orderId") Long orderId,
            @RequestParam("status") String status,
            HttpServletResponse response
    ) throws IOException {

        OrderStatus newStatus = OrderStatus.valueOf(status);
        orderService.adminMoveStatus(orderId, newStatus);

        response.sendRedirect("/admin/orders/" + orderId);
    }

    /* =========================
       PRIVATE VM BUILDERS
       ========================= */

    private List<Map<String, Object>> buildOrdersVmForTable(List<Order> orders) {
        if (orders == null) return Collections.emptyList();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        List<Map<String, Object>> res = new ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> m = new HashMap<>();

            m.put("id", o.getId());
            m.put("tableNumber", o.getTableNumber() == null ? "-" : o.getTableNumber());
            m.put("restaurantName", o.getRestaurant() != null ? o.getRestaurant().getName() : "-");
            m.put("time", o.getCreatedAt() == null ? "--:--" : o.getCreatedAt().format(timeFmt));

            int itemsCount = (o.getItems() == null) ? 0 : o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
            m.put("itemsCount", itemsCount);

            BigDecimal total = (o.getItems() == null) ? BigDecimal.ZERO : o.calculateTotalFromItems();
            m.put("total", formatMoney(total));

            // status info for badges + select
            fillStatusFlags(m, o.getStatus());

            res.add(m);
        }
        return res;
    }

    private Map<String, Object> buildSingleOrderVm(Order o) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("tableNumber", o.getTableNumber() == null ? "-" : o.getTableNumber());
        m.put("createdAt", o.getCreatedAt() == null ? "--" : o.getCreatedAt().format(timeFmt));
        m.put("restaurantName", o.getRestaurant() != null ? o.getRestaurant().getName() : "-");
        m.put("restaurantAddress", o.getRestaurant() != null && o.getRestaurant().getAddress() != null
                ? o.getRestaurant().getAddress() + (o.getRestaurant().getCity() != null ? ", " + o.getRestaurant().getCity() : "")
                : "-");
        m.put("customerNote", o.getCustomerNote() == null ? "" : o.getCustomerNote());
        m.put("hasCustomerNote", o.getCustomerNote() != null && !o.getCustomerNote().isBlank());

        List<Map<String, Object>> items = new ArrayList<>();
        if (o.getItems() != null) {
            for (OrderItem it : o.getItems()) {
                Map<String, Object> im = new HashMap<>();
                im.put("qty", it.getQuantity());
                im.put("name", it.getDish() == null ? "Dish" : it.getDish().getName());

                String note = buildItemNote(it);
                im.put("note", note);
                im.put("hasNote", note != null && !note.isBlank());

                im.put("unitPrice", formatMoneyPlain(it.getUnitPrice()));
                im.put("lineTotal", formatMoneyPlain(it.getTotalPrice()));
                items.add(im);
            }
        }

        m.put("items", items);
        m.put("hasItems", !items.isEmpty());

        BigDecimal total = (o.getItems() == null) ? BigDecimal.ZERO : o.calculateTotalFromItems();
        m.put("total", formatMoney(total));

        fillStatusFlags(m, o.getStatus());

        return m;
    }

    private void fillStatusFlags(Map<String, Object> m, OrderStatus st) {
        m.put("status", st.name());

        // Label for UI (Spanish)
        String label = switch (st) {
            case SENT_TO_KITCHEN -> "Recibida";
            case IN_PROGRESS -> "En marcha";
            case READY -> "Preparada";
            case DELIVERED -> "Entregada";
            case CANCELLED -> "Cancelada";
            default -> st.name();
        };
        m.put("statusLabel", label);

        // badge classes you already have in admin.css
        String badgeClass = switch (st) {
            case SENT_TO_KITCHEN -> "status-active";
            case IN_PROGRESS -> "status-active";
            case READY -> "status-active";
            case DELIVERED -> "status-active";
            case CANCELLED -> "status-banned";
            default -> "status-active";
        };
        m.put("statusBadgeClass", badgeClass);

        // select flags
        m.put("isSent", st == OrderStatus.SENT_TO_KITCHEN);
        m.put("isProgress", st == OrderStatus.IN_PROGRESS);
        m.put("isReady", st == OrderStatus.READY);
        m.put("isDelivered", st == OrderStatus.DELIVERED);
        m.put("isCancelled", st == OrderStatus.CANCELLED);
    }

    private String buildItemNote(OrderItem it) {
        List<String> parts = new ArrayList<>();

        if (it.getMeatPoint() != null && !it.getMeatPoint().isBlank()) {
            parts.add(it.getMeatPoint().replace("_", " ").toLowerCase());
        }
        if (it.getKitchenNote() != null && !it.getKitchenNote().isBlank()) {
            parts.add(it.getKitchenNote());
        }

        return String.join(" · ", parts);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0,00€";
        String s = value.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
        return s + "€";
    }

    private String formatMoneyPlain(BigDecimal value) {
        if (value == null) return "0,00";
        return value.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
    }
}
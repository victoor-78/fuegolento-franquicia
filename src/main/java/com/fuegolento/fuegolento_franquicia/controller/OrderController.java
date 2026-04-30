package com.fuegolento.fuegolento_franquicia.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.RestaurantRepository;
import com.fuegolento.fuegolento_franquicia.service.InvoicePdfService;
import com.fuegolento.fuegolento_franquicia.service.OrderService;
import com.fuegolento.fuegolento_franquicia.service.UserService;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final InvoicePdfService invoicePdfService;
    private final RestaurantRepository restaurantRepository;

    public OrderController(OrderService orderService,
                           UserService userService,
                           InvoicePdfService invoicePdfService,
                           RestaurantRepository restaurantRepository) {
        this.orderService = orderService;
        this.userService = userService;
        this.invoicePdfService = invoicePdfService;
        this.restaurantRepository = restaurantRepository;
    }

    /* =========================
       VIEW CART
       ========================= */

    @GetMapping
    public String orderPage(Model model, CsrfToken csrfToken, HttpSession session) {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // IMPORTANT: we don't create a new row just to show empty cart
        Restaurant restaurant = getRestaurantFromSession(session);
        Order cart = orderService.getCartIfExists(user, restaurant);

        boolean hasItems = cart != null && cart.getItems() != null && !cart.getItems().isEmpty();

        model.addAttribute("hasItems", hasItems);
        model.addAttribute("cart", cart);

        // CSRF token for Mustache templates
        if (csrfToken != null) {
            model.addAttribute("token", csrfToken.getToken());
        }

        // Build view-friendly items (so Mustache has everything ready)
        if (hasItems) {
            List<Map<String, Object>> itemsVm = buildItemsViewModel(cart.getItems());
            model.addAttribute("items", itemsVm);

            // Total for UI (not snapshot)
            model.addAttribute("total", orderService.calculateCartTotal(cart));
        } else {
            model.addAttribute("items", null);
            model.addAttribute("total", BigDecimal.ZERO);
        }

        // Table selection flags (NOW from DB)
        Integer tableNumber = (cart != null) ? cart.getTableNumber() : null;
        addTableSelectionFlags(model, tableNumber);

        // General comment (NOW from DB)
        model.addAttribute("generalComment", (cart != null && cart.getCustomerNote() != null) ? cart.getCustomerNote() : "");

        return "order"; // templates/order.html
    }

    /* =========================
       ACTIONS (CART)
       ========================= */

    @PostMapping("/add")
    public void addToCart(@RequestParam("dishId") Long dishId,
                          @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                          HttpServletResponse response,
                          HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = getRestaurantFromSession(session);
        if (restaurant == null) {
            response.sendRedirect("/restaurants");
            return;
        }

        orderService.addToCart(user, restaurant, dishId, quantity);
        response.sendRedirect("/menu");
    }

    @PostMapping("/update")
    public void updateQuantity(@RequestParam("dishId") Long dishId,
                               @RequestParam("quantity") int quantity,
                               HttpServletResponse response,
                               HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        orderService.setCartDishQuantity(user, restaurant, dishId, quantity);
        response.sendRedirect("/order");
    }

    @PostMapping("/remove")
    public void removeDish(@RequestParam("dishId") Long dishId,
                           HttpServletResponse response,
                           HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        orderService.removeFromCart(user, restaurant, dishId);
        response.sendRedirect("/order");
    }

    @PostMapping("/clear")
    public void clearCart(HttpServletResponse response, HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        orderService.clearCart(user, restaurant);
        response.sendRedirect("/order");
    }

    /* =========================
       EXTRA FORMS (NOW persisted)
       ========================= */

    @PostMapping("/comment")
    public void saveItemComment(@RequestParam("dishId") Long dishId,
                                @RequestParam(value = "comment", required = false) String comment,
                                HttpServletResponse response,
                                HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        orderService.setCartItemKitchenNote(user, restaurant, dishId, comment);
        response.sendRedirect("/order");
    }

    @PostMapping("/meat-point")
    public void saveMeatPoint(@RequestParam("dishId") Long dishId,
                              @RequestParam("meatPoint") String meatPoint,
                              HttpServletResponse response,
                              HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        orderService.setCartItemMeatPoint(user, restaurant, dishId, meatPoint);
        response.sendRedirect("/order");
    }

    @PostMapping("/table")
    public void saveTableNumber(@RequestParam("tableNumber") Integer tableNumber,
                                HttpServletResponse response,
                                HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        orderService.setCartTableNumber(user, restaurant, tableNumber);
        response.sendRedirect("/order");
    }

    @PostMapping("/general-comment")
    public void saveGeneralComment(@RequestParam(value = "generalComment", required = false) String generalComment,
                                   HttpServletResponse response,
                                   HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        orderService.setCartCustomerNote(user, restaurant, generalComment);
        response.sendRedirect("/order");
    }

    /* =========================
       SUBMIT (with table required)
       ========================= */

    @PostMapping("/submit")
    public void submitCart(Model model, CsrfToken csrfToken, HttpServletResponse response, HttpSession session) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Restaurant restaurant = requireRestaurantFromSession(session, response);
        if (restaurant == null) return;

        Order cart = orderService.getCartIfExists(user, restaurant);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            response.sendRedirect("/order?error=empty");
            return;
        }

        if (cart.getTableNumber() == null) {
            response.sendRedirect("/order?error=notableno");
            return;
        }

        orderService.submitCart(user, restaurant);
        response.sendRedirect("/order/confirmation");
    }

    @GetMapping("/confirmation")
    public String confirmationPage() {
        return "order-sent"; // templates/order-confirmation.html
    }

    /* =========================
       PRIVATE HELPERS
       ========================= */

    private List<Map<String, Object>> buildItemsViewModel(List<OrderItem> items) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (OrderItem oi : items) {
            Dish d = oi.getDish();

            Map<String, Object> vm = new HashMap<>();
            vm.put("dishId", d.getId());
            vm.put("name", d.getName());
            vm.put("description", d.getDescription());

            // Image URL (DB -> /images/{id})
            String imageUrl = "/img/menu/default-dish.png";
            if (d.getImage() != null && d.getImage().getId() != null) {
                imageUrl = "/images/" + d.getImage().getId();
            }
            vm.put("imageUrl", imageUrl);

            vm.put("quantity", oi.getQuantity());
            vm.put("unitPrice", formatMoney(oi.getUnitPrice()));
            vm.put("lineTotal", formatMoney(oi.getTotalPrice()));

            boolean isMeat = d.getCategory() == DishCategory.MEAT;
            vm.put("isMeat", isMeat);

            // Per-item comment from DB (OrderItem.kitchenNote)
            vm.put("comment", oi.getKitchenNote() == null ? "" : oi.getKitchenNote());

            // Meat point flags from DB (OrderItem.meatPoint)
            String mp = oi.getMeatPoint();

            // Default for MEAT dishes if null -> HECHO
            if (mp == null && isMeat) {
                mp = "HECHO";
            }

            vm.put("isMuyHecho", "MUY_HECHO".equals(mp));
            vm.put("isHecho", "HECHO".equals(mp));
            vm.put("isAlPunto", "AL_PUNTO".equals(mp));
            vm.put("isPocoHecho", "POCO_HECHO".equals(mp));

            result.add(vm);
        }

        return result;
    }

    @GetMapping("/{id}/invoice")
    public void downloadInvoice(@PathVariable Long id, HttpServletResponse response) throws IOException {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Order order = orderService.findById(id);

        // ✅ Ownership check (only owner can download)
        if (!order.getUser().getId().equals(user.getId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to view this invoice");
            return;
        }

        // ✅ (Recommended) Only allow invoice when delivered
        if (order.getStatus() != OrderStatus.DELIVERED) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Invoice available only for DELIVERED orders");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice_" + order.getId() + ".pdf");

        invoicePdfService.writeInvoicePdf(order, response.getOutputStream());
    }

    private void addTableSelectionFlags(Model model, Integer tableNumber) {
        model.addAttribute("tableNumber", tableNumber);

        for (int i = 1; i <= 20; i++) {
            model.addAttribute("isTable" + i, tableNumber != null && tableNumber == i);
        }
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toString();
    }

    /** Returns the Restaurant stored in session, or null if none. */
    private Restaurant getRestaurantFromSession(HttpSession session) {
        Long restaurantId = (Long) session.getAttribute("restaurantId");
        if (restaurantId == null) return null;
        return restaurantRepository.findById(restaurantId).orElse(null);
    }

    /**
     * Like getRestaurantFromSession, but writes a redirect to /restaurants
     * and returns null when no restaurant is in session — callers must return immediately.
     */
    private Restaurant requireRestaurantFromSession(HttpSession session, HttpServletResponse response) throws IOException {
        Restaurant restaurant = getRestaurantFromSession(session);
        if (restaurant == null) {
            response.sendRedirect("/restaurants");
        }
        return restaurant;
    }
}
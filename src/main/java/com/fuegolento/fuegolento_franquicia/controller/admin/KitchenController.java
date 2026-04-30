package com.fuegolento.fuegolento_franquicia.controller.admin;

import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.service.KitchenService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping
    public String kitchenBoard(Model model, CsrfToken csrfToken, HttpSession session) {

        Restaurant restaurant = (Restaurant) session.getAttribute("selectedRestaurant");

        KitchenService.KitchenBoard board = (restaurant != null)
                ? kitchenService.getBoardForRestaurant(restaurant)
                : kitchenService.getBoard();

        List<Map<String, Object>> receivedVm = buildOrdersVm(board.received);
        List<Map<String, Object>> inProgressVm = buildOrdersVm(board.inProgress);
        List<Map<String, Object>> readyVm = buildOrdersVm(board.ready);

        model.addAttribute("receivedOrders", receivedVm);
        model.addAttribute("inProgressOrders", inProgressVm);
        model.addAttribute("readyOrders", readyVm);

        model.addAttribute("countReceived", receivedVm.size());
        model.addAttribute("countProgress", inProgressVm.size());
        model.addAttribute("countDone", readyVm.size());

        model.addAttribute("hasReceived", !receivedVm.isEmpty());
        model.addAttribute("hasProgress", !inProgressVm.isEmpty());
        model.addAttribute("hasDone", !readyVm.isEmpty());

        // CSRF token for Mustache forms
        if (csrfToken != null) {
            model.addAttribute("csrfToken", csrfToken.getToken());
        }

        // For active state in header-admin (optional)
        model.addAttribute("activeKitchen", true);

        return "admin-kitchen";
    }

    /* =========================
       ACTIONS (POST)
       ========================= */

    @PostMapping("/to-in-progress")
    public void moveToInProgress(@RequestParam("orderId") Long orderId,
                                 HttpServletResponse response) throws IOException {
        kitchenService.moveToInProgress(orderId);
        response.sendRedirect("/admin/kitchen");
    }

    @PostMapping("/to-ready")
    public void moveToReady(@RequestParam("orderId") Long orderId,
                            HttpServletResponse response) throws IOException {
        kitchenService.moveToReady(orderId);
        response.sendRedirect("/admin/kitchen");
    }

    @PostMapping("/to-delivered")
    public void moveToDelivered(@RequestParam("orderId") Long orderId,
                                HttpServletResponse response) throws IOException {
        kitchenService.moveToDelivered(orderId);
        response.sendRedirect("/admin/kitchen");
    }

    @PostMapping("/back-to-received")
    public void backToReceived(@RequestParam("orderId") Long orderId,
                               HttpServletResponse response) throws IOException {
        kitchenService.backToReceived(orderId);
        response.sendRedirect("/admin/kitchen");
    }

    @PostMapping("/back-to-in-progress")
    public void backToInProgress(@RequestParam("orderId") Long orderId,
                                 HttpServletResponse response) throws IOException {
        kitchenService.backToInProgress(orderId);
        response.sendRedirect("/admin/kitchen");
    }

    /* =========================
       PRIVATE (ViewModel builders)
       ========================= */

    private List<Map<String, Object>> buildOrdersVm(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Order o : orders) {
            Map<String, Object> vm = new HashMap<>();

            vm.put("id", o.getId());
            vm.put("tableNumber", o.getTableNumber() != null ? o.getTableNumber() : "-");
            vm.put("time", o.getCreatedAt() != null ? o.getCreatedAt().format(timeFmt) : "--:--");
            vm.put("title", "Comanda #" + o.getId());

            vm.put("customerNote", o.getCustomerNote() != null ? o.getCustomerNote() : "");
            vm.put("hasCustomerNote", o.getCustomerNote() != null && !o.getCustomerNote().isBlank());

            List<Map<String, Object>> itemsVm = new ArrayList<>();
            if (o.getItems() != null) {
                for (OrderItem it : o.getItems()) {
                    Map<String, Object> itemVm = new HashMap<>();
                    itemVm.put("qty", it.getQuantity());
                    itemVm.put("name", it.getDish() != null ? it.getDish().getName() : "Dish");

                    String note = buildItemNote(it);
                    itemVm.put("note", note);
                    itemVm.put("hasNote", note != null && !note.isBlank());

                    itemsVm.add(itemVm);
                }
            }

            vm.put("items", itemsVm);
            vm.put("hasItems", !itemsVm.isEmpty());

            BigDecimal total = (o.getItems() == null) ? BigDecimal.ZERO : o.calculateTotalFromItems();
            vm.put("total", formatMoney(total));

            result.add(vm);
        }

        return result;
    }

    private String buildItemNote(OrderItem it) {
        List<String> parts = new ArrayList<>();

        if (it.getMeatPoint() != null && !it.getMeatPoint().isBlank()) {
            String mp = it.getMeatPoint().replace("_", " ").toLowerCase();
            parts.add(mp);
        }

        if (it.getKitchenNote() != null && !it.getKitchenNote().isBlank()) {
            parts.add(it.getKitchenNote());
        }

        return String.join(" · ", parts);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0,00€";
        String s = value.setScale(2, RoundingMode.HALF_UP).toString();
        s = s.replace(".", ",");
        return s + "€";
    }
}
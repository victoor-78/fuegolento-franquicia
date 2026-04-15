package com.fuegolento.fuegolento_franquicia.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.service.DishService;
import com.fuegolento.fuegolento_franquicia.service.OrderService;
import com.fuegolento.fuegolento_franquicia.service.UserService;

@Controller
public class DishController {

    private static final int MENU_PAGE_SIZE = 10;

    private final DishService dishService;
    private final OrderService orderService;
    private final UserService userService;

    public DishController(DishService dishService,
                          OrderService orderService,
                          UserService userService) {
        this.dishService = dishService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/menu")
    public String showMenu(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DishCategory category,
            Authentication authentication,
            Model model
    ) {
        boolean isLogged = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        String searchQuery = (q == null || q.trim().isEmpty()) ? "" : q.trim();
        String categoryStr = (category == null) ? "" : category.name();

        Page<Dish> firstPage = dishService.findAvailableMenuPage(searchQuery, category, 0, MENU_PAGE_SIZE);

        model.addAttribute("isLogged", isLogged);
        model.addAttribute("dishes", firstPage.getContent());
        model.addAttribute("hasMore", firstPage.hasNext());
        model.addAttribute("nextPage", 1);

        // Ensure these values are never null for Mustache
        model.addAttribute("selectedCategory", categoryStr);
        model.addAttribute("searchQuery", searchQuery);

        model.addAttribute("isStarter", category == DishCategory.STARTER);
        model.addAttribute("isMeat", category == DishCategory.MEAT);
        model.addAttribute("isDessert", category == DishCategory.DESSERT);
        model.addAttribute("isDrink", category == DishCategory.DRINK);

        if (isLogged) {
            User user = userService.getAuthenticatedUser().orElse(null);
            int cartCount = (user == null) ? 0 : orderService.getCartItemCount(user);
            model.addAttribute("cartCount", cartCount);
        } else {
            model.addAttribute("cartCount", 0);
        }

        model.addAttribute("menuActive", true);
        return "menu";
    }

    // ✅ AJAX endpoint for "Load more"
    @GetMapping("/api/menu/dishes")
    @ResponseBody
    public MenuDishesResponse menuDishesApi(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DishCategory category,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<Dish> result = dishService.findAvailableMenuPage(q, category, page, MENU_PAGE_SIZE);

        List<DishCardDto> cards = result.getContent().stream()
                .map(d -> new DishCardDto(
                        d.getId(),
                        d.getName(),
                        d.getDescription(),
                        d.getPrice(),
                        d.getImage() != null ? d.getImage().getId() : null
                ))
                .collect(Collectors.toList());

        return new MenuDishesResponse(cards, result.hasNext(), page + 1);
    }

    // Small DTOs to avoid serializing the full entity
    public static class MenuDishesResponse {
        public List<DishCardDto> items;
        public boolean hasMore;
        public int nextPage;

        public MenuDishesResponse(List<DishCardDto> items, boolean hasMore, int nextPage) {
            this.items = items;
            this.hasMore = hasMore;
            this.nextPage = nextPage;
        }
    }

    public static class DishCardDto {
        public Long id;
        public String name;
        public String description;
        public BigDecimal price;
        public Long imageId;

        public DishCardDto(Long id, String name, String description, BigDecimal price, Long imageId) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageId = imageId;
        }
    }

    @GetMapping("/menu/{id}")
    public String showDishDetail(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        boolean isLogged = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        Dish dish = dishService.findById(id);

        if (!dish.isAvailable()) {
            return "redirect:/menu";
        }

        model.addAttribute("isLogged", isLogged);
        model.addAttribute("dish", dish);
        model.addAttribute("menuActive", true);

        model.addAttribute("allergens", dish.getAllergens() == null ? List.of() : dish.getAllergens());

        if (isLogged) {
            User user = userService.getAuthenticatedUser().orElse(null);
            int cartCount = (user == null) ? 0 : orderService.getCartItemCount(user);
            model.addAttribute("cartCount", cartCount);
        } else {
            model.addAttribute("cartCount", 0);
        }

        return "dish-detail";
    }
}
package com.fuegolento.fuegolento_franquicia.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.RestaurantRepository;
import com.fuegolento.fuegolento_franquicia.service.OrderService;
import com.fuegolento.fuegolento_franquicia.service.UserService;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;
    private final OrderService orderService;
    private final RestaurantRepository restaurantRepository;

    public GlobalModelAttributes(UserService userService, OrderService orderService, RestaurantRepository restaurantRepository) {
        this.userService = userService;
        this.orderService = orderService;
        this.restaurantRepository = restaurantRepository;
    }

    @ModelAttribute("isLogged")
    public boolean isLogged(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        Collection<? extends GrantedAuthority> auths = authentication.getAuthorities();
        return auths.stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
    }


    /**
     * Exposes the selected restaurant for the navbar badge (user and admin headers use {{selectedRestaurant}}).
     * Reads restaurantId from session and loads the entity fresh from DB so it's never stale.
     */
    @ModelAttribute("selectedRestaurant")
    public Restaurant selectedRestaurant(HttpSession session) {
        Long restaurantId = (Long) session.getAttribute("restaurantId");
        if (restaurantId == null) return null;
        return restaurantRepository.findById(restaurantId).orElse(null);
    }

    /**
     * Cart badge count for the navbar.
     * Uses DB cart strategy: current cart = latest Order with status PENDING.
     */
    @ModelAttribute("cartCount")
    public int cartCount(Authentication authentication) {

        boolean logged = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!logged) return 0;

        User user = userService.getAuthenticatedUser().orElse(null);
        if (user == null) return 0;

        return orderService.getCartItemCount(user);
    }
}
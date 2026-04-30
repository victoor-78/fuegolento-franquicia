package com.fuegolento.fuegolento_franquicia.security;

import java.io.IOException;

import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.service.RestaurantService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RestaurantSuccessHandler implements AuthenticationSuccessHandler {

    private final RestaurantService restaurantService;

    public RestaurantSuccessHandler(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String restaurantIdParam = request.getParameter("restaurantId");

        if (restaurantIdParam != null && !restaurantIdParam.isBlank()) {
            try {
                Restaurant restaurant = restaurantService.findById(Long.parseLong(restaurantIdParam));
                HttpSession session = request.getSession();
                session.setAttribute("selectedRestaurant", restaurant);
                session.setAttribute("restaurantId", restaurant.getId());
            } catch (Exception e) {
                // Ignore invalid restaurantId — proceed without setting it
            }
        }

        response.sendRedirect("/menu");
    }
}

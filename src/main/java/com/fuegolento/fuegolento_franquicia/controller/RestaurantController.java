package com.fuegolento.fuegolento_franquicia.controller;

import java.util.List;

import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.repository.RestaurantRepository;
import com.fuegolento.fuegolento_franquicia.service.DishService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/restaurants")
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;
    private final DishService dishService;

    public RestaurantController(RestaurantRepository restaurantRepository, DishService dishService) {
        this.restaurantRepository = restaurantRepository;
        this.dishService = dishService;
    }

    /** Lists all active franchise locations. */
    @GetMapping
    public String listRestaurants(Model model) {
        List<Restaurant> restaurants = restaurantRepository.findByActiveTrue();
        model.addAttribute("restaurants", restaurants);
        return "restaurants";
    }

    /**
     * Shows the menu for a specific restaurant.
     * Stores the restaurantId in the HTTP session so the cart is scoped
     * to this restaurant for the rest of the user's visit.
     */
    @GetMapping("/{id}")
    public String showRestaurantMenu(@PathVariable Long id, HttpSession session, Model model) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + id));

        if (!restaurant.isActive()) {
            return "redirect:/restaurants";
        }

        // Bind the cart context and display info to this restaurant for the session
        session.setAttribute("restaurantId", restaurant.getId());
        session.setAttribute("selectedRestaurant", restaurant);

        List<Dish> dishes = dishService.findAllAvailable();

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("dishes", dishes);

        return "restaurant-menu";
    }
}

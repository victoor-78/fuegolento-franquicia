package com.fuegolento.fuegolento_franquicia.service;

import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.RestaurantDishStock;
import com.fuegolento.fuegolento_franquicia.repository.RestaurantDishStockRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StockService {

    private final RestaurantDishStockRepository stockRepository;

    public StockService(RestaurantDishStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /**
     * Returns true if the dish is available at the given restaurant.
     * If no stock entry exists, defaults to true (available by default).
     */
    public boolean isAvailable(Restaurant restaurant, Dish dish) {
        return stockRepository.findByRestaurantAndDish(restaurant, dish)
                .map(RestaurantDishStock::isAvailable)
                .orElse(true);
    }

    /**
     * Sets the availability of a dish at a given restaurant.
     * Creates a new entry if none exists.
     */
    public void setAvailable(Restaurant restaurant, Dish dish, boolean available) {
        RestaurantDishStock entry = stockRepository
                .findByRestaurantAndDish(restaurant, dish)
                .orElseGet(() -> new RestaurantDishStock(restaurant, dish, true));
        entry.setAvailable(available);
        stockRepository.save(entry);
    }

    /**
     * Returns all stock entries for a restaurant (one per dish).
     */
    public List<RestaurantDishStock> getStockForRestaurant(Restaurant restaurant) {
        return stockRepository.findByRestaurant(restaurant);
    }

    /**
     * Returns only the available stock entries for a restaurant.
     */
    public List<RestaurantDishStock> getAvailableStockForRestaurant(Restaurant restaurant) {
        return stockRepository.findByRestaurantAndAvailableTrue(restaurant);
    }

    /**
     * Returns available dishes for a restaurant.
     * A dish without a stock entry is treated as available.
     */
    public List<Dish> getAvailableDishes(Restaurant restaurant, List<Dish> allGloballyAvailableDishes) {
        // Build a set of dish IDs that are explicitly marked unavailable
        List<RestaurantDishStock> entries = stockRepository.findByRestaurant(restaurant);
        java.util.Set<Long> unavailableIds = new java.util.HashSet<>();
        for (RestaurantDishStock entry : entries) {
            if (!entry.isAvailable()) {
                unavailableIds.add(entry.getDish().getId());
            }
        }
        // Keep dishes not in the unavailable set
        return allGloballyAvailableDishes.stream()
                .filter(d -> !unavailableIds.contains(d.getId()))
                .toList();
    }

    /**
     * Ensures a stock entry exists for every (restaurant, dish) pair.
     * Used by SampleAllData on startup to initialise the stock table.
     * Only inserts rows that don't already exist.
     */
    public void ensureStockEntriesExist(List<Restaurant> restaurants, List<Dish> dishes) {
        for (Restaurant restaurant : restaurants) {
            for (Dish dish : dishes) {
                if (!stockRepository.existsByRestaurantAndDish(restaurant, dish)) {
                    stockRepository.save(new RestaurantDishStock(restaurant, dish, true));
                }
            }
        }
    }

    /**
     * Returns the stock entry for a specific restaurant+dish, or an empty Optional.
     */
    public Optional<RestaurantDishStock> findEntry(Restaurant restaurant, Dish dish) {
        return stockRepository.findByRestaurantAndDish(restaurant, dish);
    }
}

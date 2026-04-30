package com.fuegolento.fuegolento_franquicia.model;

import jakarta.persistence.*;

/**
 * Per-restaurant stock entry for a dish.
 * One row per (restaurant, dish) pair.
 * If no row exists for a given pair, the dish is considered available (defaults to true).
 */
@Entity(name = "RestaurantDishStockTable")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "dish_id"}))
public class RestaurantDishStock {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    private boolean available = true;

    public RestaurantDishStock() {}

    public RestaurantDishStock(Restaurant restaurant, Dish dish, boolean available) {
        this.restaurant = restaurant;
        this.dish = dish;
        this.available = available;
    }

    public Long getId() { return id; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public Dish getDish() { return dish; }
    public void setDish(Dish dish) { this.dish = dish; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}

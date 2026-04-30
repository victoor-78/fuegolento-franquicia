package com.fuegolento.fuegolento_franquicia.repository;

import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.RestaurantDishStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantDishStockRepository extends JpaRepository<RestaurantDishStock, Long> {

    Optional<RestaurantDishStock> findByRestaurantAndDish(Restaurant restaurant, Dish dish);

    List<RestaurantDishStock> findByRestaurant(Restaurant restaurant);

    List<RestaurantDishStock> findByRestaurantAndAvailableTrue(Restaurant restaurant);

    boolean existsByRestaurantAndDish(Restaurant restaurant, Dish dish);
}

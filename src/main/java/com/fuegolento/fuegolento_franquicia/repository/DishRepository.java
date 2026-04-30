package com.fuegolento.fuegolento_franquicia.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;

/**
 * Repository for Dish entity.
 */
public interface DishRepository extends JpaRepository<Dish, Long> {

    //AJAX
    Page<Dish> findByAvailableTrue(Pageable pageable);

    Page<Dish> findByCategoryAndAvailableTrue(DishCategory category, Pageable pageable);

    Page<Dish> findByNameContainingIgnoreCaseAndAvailableTrue(String name, Pageable pageable);

    Page<Dish> findByNameContainingIgnoreCaseAndCategoryAndAvailableTrue(String name, DishCategory category, Pageable pageable);

    // Public menu: only available dishes
    List<Dish> findByAvailableTrue();

    // Restaurant-scoped menu: only available dishes for a given restaurant
    List<Dish> findByRestaurantAndAvailableTrue(Restaurant restaurant);

    // Public menu by category: only available dishes
    List<Dish> findByCategoryAndAvailableTrue(DishCategory category);

    // Public search: only available dishes
    List<Dish> findByNameContainingIgnoreCaseAndAvailableTrue(String name);

    // Admin search (optional): includes unavailable dishes too
    List<Dish> findByCategory(DishCategory category);
    List<Dish> findByNameContainingIgnoreCase(String name);
}

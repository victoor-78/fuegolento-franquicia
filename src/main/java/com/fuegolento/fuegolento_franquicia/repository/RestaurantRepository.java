package com.fuegolento.fuegolento_franquicia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fuegolento.fuegolento_franquicia.model.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByActiveTrue();
}

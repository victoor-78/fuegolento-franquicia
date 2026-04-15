package com.fuegolento.fuegolento_franquicia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fuegolento.fuegolento_franquicia.model.Image;

/**
 * Repository for Image entity.
 */
public interface ImageRepository extends JpaRepository<Image, Long> {
}

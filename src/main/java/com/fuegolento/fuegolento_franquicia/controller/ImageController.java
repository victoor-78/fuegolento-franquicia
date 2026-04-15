package com.fuegolento.fuegolento_franquicia.controller;

import java.sql.SQLException;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fuegolento.fuegolento_franquicia.service.ImageService;

/**
 * Controller to serve images stored in the database.
 */
@Controller
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Object> getImageFile(@PathVariable long id) throws SQLException {

        Resource imageFile = imageService.getImageFile(id);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(imageFile)
                .orElse(MediaType.IMAGE_JPEG);

        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .body(imageFile);
    }
}

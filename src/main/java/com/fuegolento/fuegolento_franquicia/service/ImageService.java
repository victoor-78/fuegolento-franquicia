package com.fuegolento.fuegolento_franquicia.service;

import java.io.IOException;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fuegolento.fuegolento_franquicia.model.Image;
import com.fuegolento.fuegolento_franquicia.repository.ImageRepository;

/**
 * Service for creating and retrieving images stored as BLOBs.
 */
@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image createImage(MultipartFile imageFile) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        Image image = new Image();
        try {
            image.setImageFile(new SerialBlob(imageFile.getBytes()));
        } catch (Exception e) {
            throw new IOException("Failed to create image", e);
        }

        return imageRepository.save(image);
    }

    public Resource getImageFile(long id) throws SQLException {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));

        if (image.getImageFile() == null) {
            throw new RuntimeException("Image file not found");
        }

        return new InputStreamResource(image.getImageFile().getBinaryStream());
    }
}

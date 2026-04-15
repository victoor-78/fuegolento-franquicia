package com.fuegolento.fuegolento_franquicia.model;

import java.sql.Blob;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * Stores an image file in the database (BLOB).
 * Reusable entity so multiple domain entities can reference images
 * without duplicating upload/download logic.
 */
@Entity
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Lob
    private Blob imageFile;

    public Image() {
        // Required by JPA
    }

    public Long getId() {
        return id;
    }

    public Blob getImageFile() {
        return imageFile;
    }

    public void setImageFile(Blob imageFile) {
        this.imageFile = imageFile;
    }
}

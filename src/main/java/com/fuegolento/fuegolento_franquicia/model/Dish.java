package com.fuegolento.fuegolento_franquicia.model;

import java.math.BigDecimal;
import java.util.List;

import com.fuegolento.fuegolento_franquicia.enums.Allergen;
import com.fuegolento.fuegolento_franquicia.enums.DishCategory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * Represents a menu dish.
 * Each dish has exactly one image stored in DB using the Image entity.
 */
@Entity(name = "DishTable")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DishCategory category;

    private String name;

    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<Allergen> allergens;

    private BigDecimal price;

    private boolean available = true;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Image image;

    public Dish() {
        // Required by JPA
    }

    public Dish(DishCategory category, String name, String description, List<Allergen> allergens, BigDecimal price, boolean available) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.allergens = allergens;
        this.price = price;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public DishCategory getCategory() {
        return category;
    }

    public void setCategory(DishCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<Allergen> allergens) {
        this.allergens = allergens;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }
}
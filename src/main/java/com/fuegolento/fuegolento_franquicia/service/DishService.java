package com.fuegolento.fuegolento_franquicia.service;

import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Image;
import com.fuegolento.fuegolento_franquicia.repository.DishRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final ImageService imageService;

    public DishService(DishRepository dishRepository, ImageService imageService) {
        this.dishRepository = dishRepository;
        this.imageService = imageService;
    }

    /* =========================
       PUBLIC MENU (only available)
       ========================= */

    public List<Dish> findAllAvailable() {
        return dishRepository.findByAvailableTrue();
    }

    public List<Dish> findAvailableByCategory(DishCategory category) {
        return dishRepository.findByCategoryAndAvailableTrue(category);
    }

    public List<Dish> searchAvailableByName(String query) {
        if (query == null || query.isBlank()) {
            return findAllAvailable();
        }
        return dishRepository.findByNameContainingIgnoreCaseAndAvailableTrue(query.trim());
    }

    /**
     * ✅ SAFE pagination for Menu (AJAX "Load more")
     *
     * We intentionally keep this simple and robust to avoid Spring Data derived-query issues
     * that can break template rendering (black page).
     *
     * Strategy:
     * - Get all available dishes (simple query)
     * - Apply filters in memory (name + category)
     * - Paginate in memory
     *
     * For the practice, it is perfectly acceptable and avoids crashes.
     */
    public Page<Dish> findAvailableMenuPage(String q, DishCategory category, int page, int size) {

        String query = (q == null) ? "" : q.trim().toLowerCase();

        // 1) Load available dishes ordered by id (stable order)
        List<Dish> all = dishRepository.findByAvailableTrue();
        all.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        // 2) Filter in memory
        List<Dish> filtered = new ArrayList<>();
        for (Dish d : all) {
            boolean okQuery = query.isBlank()
                    || (d.getName() != null && d.getName().toLowerCase().contains(query));

            boolean okCategory = (category == null)
                    || (d.getCategory() == category);

            if (okQuery && okCategory) {
                filtered.add(d);
            }
        }

        // 3) Paginate in memory
        int fromIndex = page * size;
        if (fromIndex >= filtered.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size, Sort.by("id")), filtered.size());
        }

        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<Dish> pageContent = filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, PageRequest.of(page, size, Sort.by("id")), filtered.size());
    }

    /* =========================
       ADMIN (all dishes)
       ========================= */

    public List<Dish> findAll() {
        return dishRepository.findAll();
    }

    public Dish findById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dish not found with id: " + id));
    }

    public List<Dish> findByCategory(DishCategory category) {
        return dishRepository.findByCategory(category);
    }

    public List<Dish> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return dishRepository.findAll();
        }
        return dishRepository.findByNameContainingIgnoreCase(query.trim());
    }

    /* =========================
       CREATE / UPDATE / DELETE
       ========================= */

    public Dish create(Dish dish, MultipartFile imageFile) throws IOException {
        validateDish(dish);

        if (dish.getId() != null) {
            throw new IllegalArgumentException("New dish must not have an id");
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            Image img = imageService.createImage(imageFile);
            dish.setImage(img);
        }

        return dishRepository.save(dish);
    }

    public Dish update(Long id, Dish updated, MultipartFile imageFile) throws IOException {
        Dish existing = findById(id);

        validateDish(updated);

        existing.setCategory(updated.getCategory());
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setAllergens(updated.getAllergens());
        existing.setPrice(updated.getPrice());
        existing.setAvailable(updated.isAvailable());

        if (imageFile != null && !imageFile.isEmpty()) {
            Image newImg = imageService.createImage(imageFile);
            existing.setImage(newImg);
        }

        return dishRepository.save(existing);
    }

    public void deleteById(Long id) {
        if (!dishRepository.existsById(id)) {
            throw new RuntimeException("Dish not found with id: " + id);
        }
        dishRepository.deleteById(id);
    }

    /* =========================
       VALIDATION
       ========================= */

    private void validateDish(Dish dish) {
        if (dish.getCategory() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (dish.getName() == null || dish.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (dish.getPrice() == null) {
            throw new IllegalArgumentException("Price is required");
        }
        if (dish.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
    }
}
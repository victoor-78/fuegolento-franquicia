package com.fuegolento.fuegolento_franquicia.controller.admin;

import com.fuegolento.fuegolento_franquicia.enums.Allergen;
import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.service.DishService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
public class AdminDishesController {

    private final DishService dishService;

    public AdminDishesController(DishService dishService) {
        this.dishService = dishService;
    }

    /* =========================
       LIST (Search + Category filter)
       ========================= */

    @GetMapping
    public String list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) DishCategory category,
            Model model
    ) {
        // Base list (admin -> all dishes)
        List<Dish> dishes = dishService.findAll();

        // Filter by category
        if (category != null) {
            dishes = dishes.stream()
                    .filter(d -> d.getCategory() == category)
                    .collect(Collectors.toList());
        }

        // Search by name (case-insensitive)
        if (q != null && !q.isBlank()) {
            String query = q.trim().toLowerCase();
            dishes = dishes.stream()
                    .filter(d -> d.getName() != null && d.getName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        // Sort: by id desc (newer first) - you can change to name asc if you prefer
        dishes.sort(Comparator.comparing(Dish::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<Map<String, Object>> vm = buildDishesVm(dishes);

        model.addAttribute("dishes", vm);
        model.addAttribute("hasDishes", !vm.isEmpty());

        model.addAttribute("searchQuery", q == null ? "" : q);
        model.addAttribute("selectedCategory", category == null ? "" : category.name());

        // Flags for category select
        model.addAttribute("isStarter", category == DishCategory.STARTER);
        model.addAttribute("isMeat", category == DishCategory.MEAT);
        model.addAttribute("isDessert", category == DishCategory.DESSERT);
        model.addAttribute("isDrink", category == DishCategory.DRINK);

        // For header-admin active item
        model.addAttribute("activeProducts", true);

        return "admin-products"; // templates/admin-products.html
    }

    /* =========================
       PRIVATE VM
       ========================= */

    private List<Map<String, Object>> buildDishesVm(List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> res = new ArrayList<>();

        for (Dish d : dishes) {
            Map<String, Object> m = new HashMap<>();

            m.put("id", d.getId());
            m.put("name", safe(d.getName()));
            m.put("description", safe(d.getDescription()));

            // Category label (Spanish)
            m.put("categoryLabel", categoryLabel(d.getCategory()));

            // Price formatted (comma + €)
            m.put("price", formatMoney(d.getPrice()));

            // Availability badge
            boolean available = d.isAvailable();
            m.put("availableLabel", available ? "Sí" : "No");
            // Reuse your existing classes (status-active / status-banned)
            m.put("availableBadgeClass", available ? "status-active" : "status-banned");

            // Image URL (from DB image if exists)
            String imageUrl = "/img/menu/default-dish.png";
            if (d.getImage() != null && d.getImage().getId() != null) {
                imageUrl = "/images/" + d.getImage().getId();
            }
            m.put("imageUrl", imageUrl);

            res.add(m);
        }

        return res;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String categoryLabel(DishCategory c) {
        if (c == null) return "-";
        return switch (c) {
            case STARTER -> "Entrantes";
            case MEAT -> "Carnes";
            case DESSERT -> "Postres";
            case DRINK -> "Bebidas";
        };
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0,00€";
        String s = value.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
        return s + "€";
    }
    
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("activeProducts", true);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", defaultForm());
        }

        Map<String, Object> form = castForm(model.getAttribute("form"));

        addCategoryFlags(model, (String) form.get("category"));
        addAllergenFlags(model, castStringList(form.get("allergens")));

        // For the preview (new -> default logo)
        model.addAttribute("previewImageUrl", "/img/logo.png");
        model.addAttribute("modeLabelUpper", "Crear producto");
        model.addAttribute("pageTitle", "Nuevo producto");
        model.addAttribute("submitLabel", "Crear producto");

        return "admin-product-new";
    }

    @PostMapping("/new")
    public String create(
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("price") String price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "available", required = false) String available,
            @RequestParam(value = "allergens", required = false) List<String> allergens,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra
    ) {
        Map<String, Object> form = buildFormFromRequest(name, category, price, description, available, allergens);

        try {
            Dish dish = mapFormToDish(form);
            dishService.create(dish, imageFile);

            ra.addFlashAttribute("successMessage", "Producto creado correctamente.");
            return "redirect:/admin/products";

        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IOException ex) {
            ra.addFlashAttribute("errorMessage", "Error subiendo la imagen.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "No se pudo crear el producto.");
        }

        ra.addFlashAttribute("form", form);
        return "redirect:/admin/products/new";
    }

    /* =========================
       EDIT (FORM)
       ========================= */

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("activeProducts", true);

        Dish dish = dishService.findById(id);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", buildFormFromDish(dish));
        }

        Map<String, Object> form = castForm(model.getAttribute("form"));

        addCategoryFlags(model, (String) form.get("category"));
        addAllergenFlags(model, castStringList(form.get("allergens")));

        // Preview image: dish image if exists, else fallback
        String previewUrl = "/img/logo.png";
        if (dish.getImage() != null && dish.getImage().getId() != null) {
            previewUrl = "/images/" + dish.getImage().getId();
        }
        model.addAttribute("previewImageUrl", previewUrl);

        // Meta
        model.addAttribute("dishId", id);
        model.addAttribute("dishName", dish.getName() == null ? "" : dish.getName());
        model.addAttribute("modeLabelUpper", "Editar producto");
        model.addAttribute("pageTitle", "Editor de producto");
        model.addAttribute("submitLabel", "Guardar cambios");

        return "admin-product-edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("price") String price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "available", required = false) String available,
            @RequestParam(value = "allergens", required = false) List<String> allergens,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra
    ) {
        Map<String, Object> form = buildFormFromRequest(name, category, price, description, available, allergens);

        try {
            Dish updated = mapFormToDish(form);
            dishService.update(id, updated, imageFile);

            ra.addFlashAttribute("successMessage", "Cambios guardados correctamente.");
            return "redirect:/admin/products";

        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IOException ex) {
            ra.addFlashAttribute("errorMessage", "Error subiendo la imagen.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "No se pudo guardar el producto.");
        }

        ra.addFlashAttribute("form", form);
        return "redirect:/admin/products/" + id + "/edit";
    }

    /* =========================
       DELETE (OPTIONAL)
       ========================= */

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            dishService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Producto eliminado.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "No se pudo eliminar el producto.");
        }
        return "redirect:/admin/products";
    }

    /* =========================
       PRIVATE HELPERS
       ========================= */

    private Map<String, Object> defaultForm() {
        Map<String, Object> form = new HashMap<>();
        form.put("name", "");
        form.put("description", "");
        form.put("price", "");
        form.put("available", true);
        form.put("category", "");
        form.put("allergens", List.of());
        return form;
    }

    private Map<String, Object> buildFormFromRequest(
            String name,
            String category,
            String price,
            String description,
            String available,
            List<String> allergens
    ) {
        Map<String, Object> form = new HashMap<>();
        form.put("name", name == null ? "" : name.trim());
        form.put("description", description == null ? "" : description.trim());
        form.put("price", price == null ? "" : price.trim());
        form.put("category", category == null ? "" : category.trim());
        form.put("available", available != null); // checkbox: param exists only if checked
        form.put("allergens", allergens == null ? List.of() : allergens);
        return form;
    }

    private Map<String, Object> buildFormFromDish(Dish d) {
        Map<String, Object> form = new HashMap<>();
        form.put("name", d.getName() == null ? "" : d.getName());
        form.put("description", d.getDescription() == null ? "" : d.getDescription());
        form.put("price", d.getPrice() == null ? "" : formatPricePlain(d.getPrice()));
        form.put("category", d.getCategory() == null ? "" : d.getCategory().name());
        form.put("available", d.isAvailable());

        List<String> al = (d.getAllergens() == null)
                ? List.of()
                : d.getAllergens().stream().map(Enum::name).collect(Collectors.toList());
        form.put("allergens", al);

        return form;
    }

    private Dish mapFormToDish(Map<String, Object> form) {
        Dish dish = new Dish();

        String name = String.valueOf(form.get("name")).trim();
        dish.setName(name);

        String category = String.valueOf(form.get("category")).trim();
        dish.setCategory(DishCategory.valueOf(category));

        String priceRaw = String.valueOf(form.get("price")).trim();
        dish.setPrice(parsePrice(priceRaw));

        dish.setDescription(String.valueOf(form.get("description")));

        dish.setAvailable(Boolean.TRUE.equals(form.get("available")));

        List<String> allergens = castStringList(form.get("allergens"));
        if (allergens == null || allergens.isEmpty()) {
            dish.setAllergens(List.of());
        } else {
            List<Allergen> al = allergens.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Allergen::valueOf)
                    .collect(Collectors.toList());
            dish.setAllergens(al);
        }

        return dish;
    }

    private BigDecimal parsePrice(String raw) {
        if (raw == null) throw new IllegalArgumentException("Price is required");

        String s = raw.trim().replace(",", ".");
        if (s.isEmpty()) throw new IllegalArgumentException("Price is required");

        try {
            return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid price format");
        }
    }

    private String formatPricePlain(BigDecimal value) {
        if (value == null) return "";
        return value.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
    }

    private void addCategoryFlags(Model model, String selected) {
        model.addAttribute("catStarter", "STARTER".equals(selected));
        model.addAttribute("catMeat", "MEAT".equals(selected));
        model.addAttribute("catDessert", "DESSERT".equals(selected));
        model.addAttribute("catDrink", "DRINK".equals(selected));
    }

    private void addAllergenFlags(Model model, List<String> selected) {
        Set<String> set = (selected == null) ? Set.of() : new HashSet<>(selected);

        model.addAttribute("algGluten", set.contains("GLUTEN"));
        model.addAttribute("algEgg", set.contains("EGG"));
        model.addAttribute("algMilk", set.contains("MILK"));
        model.addAttribute("algNuts", set.contains("NUTS"));
        model.addAttribute("algFish", set.contains("FISH"));
        model.addAttribute("algShellfish", set.contains("SHELLFISH"));
        model.addAttribute("algSoy", set.contains("SOY"));
        model.addAttribute("algSesame", set.contains("SESAME"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castForm(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return defaultForm();
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object obj) {
        if (obj == null) return List.of();
        if (obj instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
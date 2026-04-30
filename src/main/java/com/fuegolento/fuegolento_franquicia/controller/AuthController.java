package com.fuegolento.fuegolento_franquicia.controller;

import java.time.LocalDate;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.service.RestaurantService;
import com.fuegolento.fuegolento_franquicia.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;
    private final RestaurantService restaurantService;

    public AuthController(UserService userService, RestaurantService restaurantService) {
        this.userService = userService;
        this.restaurantService = restaurantService;
    }

    /* =========================
       LOGIN PAGES
       ========================= */

    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {

        // If already authenticated, go to profile
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/profile";
        }

        model.addAttribute("restaurants", restaurantService.findAllActive());
        model.addAttribute("activeTab", "login");
        return "login";
    }

    @GetMapping("/loginerror")
    public String loginError(Model model) {
        model.addAttribute("restaurants", restaurantService.findAllActive());
        model.addAttribute("error", "Usuario o contraseña incorrectos.");
        model.addAttribute("activeTab", "login");
        return "login";
    }

    @GetMapping("/banned")
    public String banned(Model model) {
        model.addAttribute("restaurants", restaurantService.findAllActive());
        model.addAttribute("error", "Tu cuenta ha sido bloqueada. Contacta con el administrador.");
        model.addAttribute("activeTab", "login");
        return "login";
    }

    /* =========================
       REGISTER (POST)
       ========================= */

    @PostMapping("/register")
    public String register(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("password2") String password2,
            @RequestParam(value = "birthDate", required = false) String birthDate,
            Model model
    ) {
        model.addAttribute("activeTab", "register");

        // Trim inputs
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim();

        // Basic checks
        if (username.isBlank()) {
            model.addAttribute("registerError", "El nombre de usuario es obligatorio.");
            return "login";
        }

        if (email.isBlank()) {
            model.addAttribute("registerError", "El email es obligatorio.");
            return "login";
        }

        if (password == null || password.isBlank()) {
            model.addAttribute("registerError", "La contraseña es obligatoria.");
            return "login";
        }

        if (!password.equals(password2)) {
            model.addAttribute("registerError", "Las contraseñas no coinciden.");
            return "login";
        }

        // Build User with RAW password in encodedPassword field
        // (UserService.registerUser will encode it)
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setEncodedPassword(password);

        // Optional birthDate
        if (birthDate != null && !birthDate.isBlank()) {
            try {
                user.setBirthDate(LocalDate.parse(birthDate));
            } catch (Exception e) {
                model.addAttribute("registerError", "La fecha de nacimiento no es válida.");
                return "login";
            }
        }

        try {
            userService.registerUser(user);
        } catch (IllegalArgumentException ex) {
            // Your service throws IllegalArgumentException with clear messages
            model.addAttribute("registerError", ex.getMessage());
            return "login";
        }

        // Success -> show login tab with message
        model.addAttribute("activeTab", "login");
        model.addAttribute("registerOk", "Cuenta creada correctamente. Ya puedes iniciar sesión.");
        return "login";
    }
}
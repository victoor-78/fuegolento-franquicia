package com.fuegolento.fuegolento_franquicia.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.service.UserService;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication,
                          Model model,
                          CsrfToken csrfToken,
                          @RequestParam(value = "updated", required = false) String updated) {

        Optional<User> optUser = userService.getAuthenticatedUser();

        if (optUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = optUser.get();

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());

        String birthDateStr = (user.getBirthDate() != null) ? user.getBirthDate().toString() : "";
        model.addAttribute("birthDate", birthDateStr);

        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        boolean isUser = user.getRoles() != null && user.getRoles().contains("USER");

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isUser", isUser);

        model.addAttribute("updated", updated != null);

        if (csrfToken != null) {
            model.addAttribute("token", csrfToken.getToken());
        }

        model.addAttribute("profileActive", true);

        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(Model model, CsrfToken csrfToken) {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());

        String birthDateStr = (user.getBirthDate() != null) ? user.getBirthDate().toString() : "";
        model.addAttribute("birthDate", birthDateStr);

        if (csrfToken != null) {
            model.addAttribute("token", csrfToken.getToken());
        }

        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String editProfileSubmit(@RequestParam("email") String email,
                                    @RequestParam(value = "birthDate", required = false) String birthDate,
                                    Model model,
                                    CsrfToken csrfToken) {

        User user = userService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        try {
            userService.updateProfile(user, email, birthDate);
            return "redirect:/profile?updated=1";
        } catch (IllegalArgumentException ex) {

            // Repaint form with attempted values + error message
            model.addAttribute("user", user);
            model.addAttribute("username", user.getUsername());
            model.addAttribute("email", email != null ? email.trim() : "");
            model.addAttribute("birthDate", birthDate != null ? birthDate.trim() : "");
            model.addAttribute("error", ex.getMessage());

            if (csrfToken != null) {
                model.addAttribute("token", csrfToken.getToken());
            }

            return "profile-edit";
        }
    }
}
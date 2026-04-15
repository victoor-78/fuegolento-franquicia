package com.fuegolento.fuegolento_franquicia.controller.admin;

import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

    private final UserService userService;

    public AdminUsersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(@RequestParam(value = "q", required = false) String q,
                            Model model) {

        List<User> users = (q == null || q.isBlank())
                ? userService.findAll()
                : userService.searchUsers(q);

        model.addAttribute("users", users);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("activeUsers", true);

        return "admin-users";
    }

    @PostMapping("/{id}/ban")
    public void banUser(@PathVariable Long id,
                        HttpServletResponse response) throws IOException {

        userService.banUser(id);

        response.sendRedirect("/admin/users");
    }

    @PostMapping("/{id}/unban")
    public void unbanUser(@PathVariable Long id,
                          HttpServletResponse response) throws IOException {

        userService.unbanUser(id);

        response.sendRedirect("/admin/users");
    }
}
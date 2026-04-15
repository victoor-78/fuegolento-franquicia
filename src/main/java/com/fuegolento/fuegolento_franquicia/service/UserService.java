package com.fuegolento.fuegolento_franquicia.service;

import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* =========================
       BASIC CRUD
       ========================= */

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    public List<User> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
}

    /* =========================
       AUTHENTICATED USER
       ========================= */

    public Optional<User> getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername());
        }

        String username = authentication.getName();
        if (username == null || username.equals("anonymousUser")) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username);
    }

    /* =========================
       REGISTER
       ========================= */

    public User registerUser(User user) {

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (user.getEncodedPassword() == null || user.getEncodedPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Encode raw password
        user.setEncodedPassword(passwordEncoder.encode(user.getEncodedPassword()));

        // Default role
        user.setRoles(List.of("USER"));

        // New users are not banned
        user.setBanned(false);

        return userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User user, String email, String birthDate) {

        if (user == null) throw new IllegalArgumentException("User is required");

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        user.setEmail(email.trim());

        if (birthDate != null && !birthDate.trim().isEmpty()) {
            // Expected format from <input type="date"> is yyyy-MM-dd
            user.setBirthDate(java.time.LocalDate.parse(birthDate.trim()));
        } else {
            user.setBirthDate(null);
        }

        userRepository.save(user);
    }

    /* =========================
       BAN / UNBAN
       ========================= */

    public void banUser(Long userId) {
        User user = findById(userId);
        user.setBanned(true);
        userRepository.save(user);
    }

    public void unbanUser(Long userId) {
        User user = findById(userId);
        user.setBanned(false);
        userRepository.save(user);
    }
}

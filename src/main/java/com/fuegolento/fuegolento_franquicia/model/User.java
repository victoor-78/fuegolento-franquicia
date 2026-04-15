package com.fuegolento.fuegolento_franquicia.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

/**
 * Represents an application user.
 * Passwords are stored encoded (never in plain text).
 */
@Entity(name = "UserTable")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // Username used to log into the application
    private String username;

    // User email address
    private String email;

    // User birth date
    private LocalDate birthDate;

    // Encoded password (BCrypt)
    private String encodedPassword;

    // User roles (e.g. USER, ADMIN)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;

    // Indicates whether the user is banned (cannot log in)
    private boolean banned = false;

    // User registration date/time (for analytics dashboards)
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public User() {
        // Required by JPA
    }

    public User(String username, String email, LocalDate birthDate, String encodedPassword, String... roles) {
        this.username = username;
        this.email = email;
        this.birthDate = birthDate;
        this.encodedPassword = encodedPassword;
        this.roles = List.of(roles);
        this.banned = false;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
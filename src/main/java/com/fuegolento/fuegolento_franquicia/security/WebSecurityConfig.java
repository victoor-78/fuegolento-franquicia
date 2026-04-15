package com.fuegolento.fuegolento_franquicia.security;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private RepositoryUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(authenticationProvider());

        http
            .authorizeHttpRequests(authorize -> authorize

                // ===============================
                // PUBLIC (not authenticated)
                // ===============================
                .requestMatchers("/", "/index").permitAll()
                .requestMatchers("/our-grill", "/gallery", "/contact", "/booking").permitAll()

                // Menu (public)
                .requestMatchers("/menu", "/menu/**").permitAll()
                .requestMatchers("/dish", "/dish/**").permitAll()

                // AJAX endpoints (public)
                .requestMatchers("/api/menu/**").permitAll()

                // Auth pages (public)
                .requestMatchers("/login", "/register", "/loginerror", "/banned").permitAll()

                // IMPORTANT: allow /error to everyone (avoid loops)
                .requestMatchers("/error").permitAll()

                // Static resources (public)
                .requestMatchers(
                    "/assets/**",
                    "/images/**",
                    "/img/**",
                    "/vendor/**",
                    "/css/**",
                    "/js/**",
                    "/favicon.ico"
                ).permitAll()

                // ===============================
                // USER (authenticated)
                // ===============================
                .requestMatchers(
                    "/profile",
                    "/profile/**",
                    "/order",
                    "/order/**",
                    "/orders",
                    "/orders/**"
                ).hasRole("USER")

                // ===============================
                // ADMIN
                // ===============================
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")

                .anyRequest().permitAll()
            )
            
            .exceptionHandling(ex -> ex.accessDeniedHandler(
                (HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) -> {
                    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
                    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Forbidden");
                    request.getRequestDispatcher("/error").forward(request, response);
                }
            ))

            // ===============================
            // Login / Logout
            // ===============================
            .formLogin(formLogin -> formLogin
                .loginPage("/login")
                .defaultSuccessUrl("/profile", true)
                .failureHandler((request, response, exception) -> {
                    if (exception instanceof org.springframework.security.authentication.DisabledException) {
                        response.sendRedirect("/banned");
                    } else {
                        response.sendRedirect("/loginerror");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
package com.fuegolento.fuegolento_franquicia.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model, Authentication authentication) {

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        boolean isLogged = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("isLogged", isLogged);

        String code = "500";
        int statusCode = 500;

        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
                code = String.valueOf(statusCode);
            } catch (NumberFormatException ignored) {
                // keep 500
            }
        }

        model.addAttribute("errorCode", code);

        if (statusCode == 404) {
            model.addAttribute("errorTitle", "Página no encontrada");
            model.addAttribute("errorMessage", "La URL que has escrito no existe o se ha movido.");
            model.addAttribute("primaryCtaText", "Volver al inicio");
            model.addAttribute("primaryCtaHref", "/");
            model.addAttribute("secondaryCtaText", "Ver menú");
            model.addAttribute("secondaryCtaHref", "/menu");
            return "error/error";
        }

        if (statusCode == 403) {
            model.addAttribute("errorTitle", "Acceso denegado");
            model.addAttribute("errorMessage", "No tienes permisos para acceder a esta sección.");
            model.addAttribute("primaryCtaText", "Volver al inicio");
            model.addAttribute("primaryCtaHref", "/");

            // Si NO está logueado, tiene sentido dar botón de login.
            // Si está logueado (USER intentando entrar a admin), mejor ir a perfil.
            if (isLogged) {
                model.addAttribute("secondaryCtaText", "Ir a mi perfil");
                model.addAttribute("secondaryCtaHref", "/profile");
            } else {
                model.addAttribute("secondaryCtaText", "Iniciar sesión");
                model.addAttribute("secondaryCtaHref", "/login");
            }

            return "error/error";
        }

        // Generic
        model.addAttribute("errorTitle", "Ha ocurrido un error");
        model.addAttribute("errorMessage", "Se ha producido un problema inesperado. Inténtalo de nuevo.");
        model.addAttribute("primaryCtaText", "Volver al inicio");
        model.addAttribute("primaryCtaHref", "/");
        model.addAttribute("secondaryCtaText", "Ver menú");
        model.addAttribute("secondaryCtaHref", "/menu");
        return "error/error";
    }
}
package com.fuegolento.fuegolento_franquicia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the home page and public sections.
 */
@Controller
public class HomeController {

    /**
     * Home page (index)
     */
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    /**
     * Alternative mapping for index
     */
    @GetMapping("/index")
    public String indexAlt(Model model) {
        return "index";
    }
}

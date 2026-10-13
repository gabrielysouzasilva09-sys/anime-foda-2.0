package com.animeplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    /**
     * Redireciona "/" para o index.html estático em src/main/resources/static/.
     * Usar "forward:" serve o ficheiro directamente sem mudar a URL no browser.
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}

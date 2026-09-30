package com.example.shop.api.controller;

import com.example.shop.api.service.HomeReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HomeApiController {
    private final HomeReadService home;

    public HomeApiController(HomeReadService home) {
        this.home = home;
    }

    /** The home page blocks below the banner; public. */
    @GetMapping("/home")
    public Map<String, Object> home() {
        return home.home();
    }
}

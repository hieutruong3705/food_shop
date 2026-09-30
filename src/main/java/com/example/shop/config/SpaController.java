package com.example.shop.config;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
/** Built Vue assets are copied to classpath:/static by Maven after npm run build. */
@Controller
public class SpaController {
    @GetMapping({"/", "/products", "/products/{id}", "/login", "/register", "/verify", "/forgot-password", "/reset-password", "/cart", "/checkout", "/account", "/orders", "/orders/{id}", "/policies", "/payment-return", "/forbidden", "/admin", "/admin/{resource}"})
    public String index(){return "forward:/index.html";}
}

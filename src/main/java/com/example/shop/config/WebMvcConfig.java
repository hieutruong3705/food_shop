package com.example.shop.config;
import com.example.shop.api.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor rateLimit;
    public WebMvcConfig(RateLimitInterceptor rateLimit) { this.rateLimit = rateLimit; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimit).addPathPatterns(RateLimitInterceptor.PATHS);
    }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
    }
}

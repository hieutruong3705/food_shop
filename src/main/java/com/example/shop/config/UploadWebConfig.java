package com.example.shop.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class UploadWebConfig implements WebMvcConfigurer {
    @Value("${shop.upload-dir:./data/uploads}") private String directory;
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry){registry.addResourceHandler("/uploads/**").addResourceLocations(java.nio.file.Path.of(directory).toAbsolutePath().normalize().toUri().toString()+"/");}
}

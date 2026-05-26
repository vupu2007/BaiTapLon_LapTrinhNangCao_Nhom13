package com.auction.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Biến thư mục cứng "C:/auction_images/" trên Server thành đường dẫn URL công khai "/uploads/**"
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/auction_images/");
    }
}
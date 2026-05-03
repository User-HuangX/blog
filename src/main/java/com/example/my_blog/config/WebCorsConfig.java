package com.example.my_blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 前端 fetch 使用 credentials: include 时必须 allowCredentials(true)，且不能用 * 作为 Origin。
 * localhost 与 127.0.0.1、IPv6 环回是不同源，需分别允许（用 pattern 带任意端口）。
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final List<String> extraOriginPatterns;

    public WebCorsConfig(
            @Value("${app.cors.extra-origin-patterns:}") String extraOriginPatterns
    ) {
        this.extraOriginPatterns = new ArrayList<>();
        if (StringUtils.hasText(extraOriginPatterns)) {
            Arrays.stream(extraOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(this.extraOriginPatterns::add);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> patterns = new ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://[::1]:*"
        ));
        patterns.addAll(extraOriginPatterns);

        registry.addMapping("/api/**")
                .allowedOriginPatterns(patterns.toArray(String[]::new))
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

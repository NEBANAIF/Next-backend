package com.ousman.config;

/**
 * CORS is configured exclusively in {@link SecurityConfig#corsSource()}.
 * This file is intentionally empty — do not add a duplicate WebMvcConfigurer
 * bean here as it conflicts with the Spring Security CORS filter chain.
 */
public class CorsConfig {
    // Intentionally empty — see SecurityConfig
}

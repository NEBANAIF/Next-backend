package com.ousman.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  Security Configuration — Role-Based Access Control
 *  ─────────────────────────────────────────────────────────────────────────
 *
 *  ROLES:
 *    ADMIN  → full access to every endpoint
 *    WORKER → full Products access (read, create, update, delete, stock-adjust),
 *             read today's Sales only, POST sales (record)
 *             NO access to: users, analytics, finance, stock-history, delete sales
 *
 *  JWT is stateless (no session). Role is embedded in token claim "role".
 *  JwtAuthFilter extracts it and sets ROLE_ADMIN or ROLE_WORKER authority.
 *
 *  @EnableMethodSecurity is also enabled so controllers can use @PreAuthorize
 *  for fine-grained per-method control.
 * ─────────────────────────────────────────────────────────────────────────
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // enables @PreAuthorize on controller methods
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── 401 handler: returns JSON instead of HTML ──────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(401);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"error\":\"Authentication required\"}");
                })
                // ── 403 handler: returns JSON for role-denied requests ─────
                .accessDeniedHandler((req, res, deniedException) -> {
                    res.setStatus(403);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"error\":\"Access denied: insufficient permissions\"}");
                })
            )

            .authorizeHttpRequests(auth -> auth

                // ── Public endpoints (no token required) ──────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/setup/**").permitAll()  // Allow initial admin creation
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/ping").permitAll()  // lightweight keep-alive ping (cron-job.org)

                // ── ADMIN-ONLY: User management ────────────────────────────
                // Workers cannot view, create, update or delete users
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // ── Analytics & Finance (Expenses) ─────────────────────────
                // Analytics/dashboard is available to ADMIN and the three
                // location-scoped roles (their view is auto-filtered to their
                // own branch(es) by AnalyticsService); Finance/Expenses stays
                // ADMIN-only since expenses aren't attributed to a branch.
                // The legacy WORKER role keeps its original restriction on both.
                .requestMatchers("/api/analytics/**")
                    .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "STORE_MANAGER", "STAFF")
                .requestMatchers("/api/expenses/**").hasRole("ADMIN")

                // ── Stock history: ADMIN and the three location-scoped roles ──
                // (WAREHOUSE_MANAGER/STORE_MANAGER/STAFF) can view — each is
                // further filtered to their own branch(es) by the service
                // layer (AccessControlService). The legacy WORKER role keeps
                // its original restriction: no stock-history access at all.
                .requestMatchers(HttpMethod.DELETE, "/api/stock-history/**").hasRole("ADMIN")
                .requestMatchers("/api/stock-history/**")
                    .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "STORE_MANAGER", "STAFF")

                // ── SALES: Workers can GET (today filter done in frontend)
                //           Workers can POST (record a sale)
                //           Workers CANNOT DELETE — ADMIN only
                .requestMatchers(HttpMethod.DELETE, "/api/sales/**").hasRole("ADMIN")
                .requestMatchers("/api/sales/**").authenticated()  // GET + POST allowed for both roles

                // ── Customers: every operational role, branch-scoped by the
                //    service layer for scoped roles ─────────────────────────
                .requestMatchers("/api/customers/**").authenticated()

                // ── Suppliers (global) & Purchases (branch-scoped): every
                //    operational role; supplier delete is ADMIN-only ────────
                .requestMatchers(HttpMethod.DELETE, "/api/suppliers/**").hasRole("ADMIN")
                .requestMatchers("/api/suppliers/**").authenticated()
                .requestMatchers("/api/purchases/**").authenticated()

                // ── PRODUCTS: Both ADMIN and WORKER have full access
                //              (read, create, update, delete, stock-adjust)
                // Products: both roles can read/create/edit/stock-adjust; only ADMIN can delete
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers("/api/products/**").authenticated()

                // ── Everything else requires authentication ─────────────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow all origins (requests are proxied through Nginx)
        // In a multi-domain setup, parse from app.cors.allowed-origins env variable
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
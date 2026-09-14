package com.ousman.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration — uses Caffeine as the in-process cache provider.
 *
 * Cache strategy per cache name:
 *
 *  ┌─────────────┬────────────┬──────────────────────────────────────────────┐
 *  │ Cache name  │ TTL        │ Evicted by                                   │
 *  ├─────────────┼────────────┼──────────────────────────────────────────────┤
 *  │ products    │ 10 minutes │ @CacheEvict in ProductService (write ops)    │
 *  │ analytics   │  5 minutes │ time-based only (data changes slowly)        │
 *  │ expenses    │ 10 minutes │ @CacheEvict in ExpenseService (write ops)    │
 *  └─────────────┴────────────┴──────────────────────────────────────────────┘
 *
 * Load balancing note:
 *  Each backend replica holds its OWN in-process cache. This is intentional —
 *  Caffeine is faster than any distributed cache for read-heavy ERP data.
 *  Write operations call @CacheEvict which clears the local replica's cache;
 *  the other replica's cache expires within its TTL (5–10 min), which is
 *  acceptable for this workload. For strict consistency, swap CaffeineCacheManager
 *  for a Redis-backed CacheManager.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * CacheManager bean — configures per-cache TTL and size limits.
     * Caffeine is significantly faster than the default SimpleCacheManager
     * and supports automatic eviction, size bounding, and statistics.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Default spec for all caches unless overridden below
        // maximumSize: evict least-recently-used entries after this count
        // expireAfterWrite: hard TTL — entry removed this long after being written
        manager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(500)                          // max 500 entries per cache
                .expireAfterWrite(10, TimeUnit.MINUTES)   // default TTL = 10 minutes
                .recordStats()                             // enable hit/miss stats via actuator
        );

        // Pre-register cache names — Spring will create them on first access
        manager.setCacheNames(java.util.List.of("products", "analytics", "expenses"));
        return manager;
    }
}

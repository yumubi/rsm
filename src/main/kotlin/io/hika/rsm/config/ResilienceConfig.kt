package io.hika.rsm.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class ResilienceConfig {
    @Bean
    fun circuitBreaker(): CircuitBreaker {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(100)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .build()

        val registry = CircuitBreakerRegistry.of(circuitBreakerConfig)
        return registry.circuitBreaker("state-replication")
    }
}

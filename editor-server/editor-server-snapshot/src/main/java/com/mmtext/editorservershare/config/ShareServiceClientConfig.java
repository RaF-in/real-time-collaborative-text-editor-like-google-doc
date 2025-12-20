package com.mmtext.editorservershare.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Share Service Client resilience patterns
 */
@Configuration
public class ShareServiceClientConfig {

    @Value("${grpc.share.service.timeout:5}")
    private int timeoutSeconds;

    @Value("${grpc.share.service.circuit-breaker.failureRateThreshold:50}")
    private int failureRateThreshold;

    @Value("${grpc.share.service.circuit-breaker.waitDuration:30}")
    private int waitDuration;

    @Value("${grpc.share.service.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    @Value("${grpc.share.service.retry.waitDuration:1}")
    private int retryWaitDuration;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDuration))
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        registry.circuitBreaker("shareService");
        registry.circuitBreaker("shareServiceHealth");

        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetryAttempts)
                .waitDuration(Duration.ofSeconds(retryWaitDuration))
                .retryExceptions(
                    io.grpc.StatusRuntimeException.class,
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class
                )
                .ignoreExceptions(
                    io.grpc.StatusRuntimeException.class // Let the method handle specific gRPC errors
                )
                .build();

        RetryRegistry registry = RetryRegistry.of(retryConfig);
        registry.retry("shareService");

        return registry;
    }

    @Bean
    public TimeLimiter timeLimiter() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(timeoutSeconds))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiter.of(timeLimiterConfig);
    }

    @Bean
    public CircuitBreaker shareServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("shareService");
    }

    @Bean
    public Retry shareServiceRetry(RetryRegistry registry) {
        return registry.retry("shareService");
    }

    @Bean
    public CircuitBreaker shareServiceHealthCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("shareServiceHealth");
    }
}
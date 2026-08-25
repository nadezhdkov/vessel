package io.vessel.benchmark.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring-equivalent of {@code VesselStartupBenchmark}: same idea (build a DI
 * container, scan a package of N generated {@code @Component} classes,
 * measure wall-clock time until it's ready), using {@code spring-context}
 * directly rather than full Spring Boot — Vessel itself has no
 * auto-configuration layer to compare against (a deliberate CLAUDE.md
 * boundary), so comparing raw DI-container startup against raw DI-container
 * startup is the fairer, more apples-to-apples measurement.
 */
public final class SpringStartupBenchmark {

    @Configuration
    @ComponentScan("generated")
    static class BenchmarkConfig {
    }

    private SpringStartupBenchmark() {
    }

    public static void main(String[] args) {
        long startNanos = System.nanoTime();

        var context = new AnnotationConfigApplicationContext(BenchmarkConfig.class);

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        System.out.println("Spring: %d bean definitions, context ready in %dms"
                .formatted(context.getBeanDefinitionCount(), elapsedMillis));

        context.close();
    }
}

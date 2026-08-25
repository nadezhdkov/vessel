package io.vessel.benchmark;

import io.vessel.core.Container;

/**
 * Measures wall-clock time to build a {@link Container}, scan the generated
 * {@code generated} package (one real class per bean, produced by the
 * {@code generateBeans} Gradle task — see build.gradle.kts), and eagerly
 * resolve every one of them via {@link Container#resolveAll()} — the same
 * "validate the whole graph at startup" call {@code Vessel.start()} (M10)
 * makes. A single cold measurement, not an average over warmed-up
 * iterations: this benchmark is about time-to-first-request, the number a
 * real deployment actually experiences once, not steady-state throughput.
 */
public final class VesselStartupBenchmark {

    private VesselStartupBenchmark() {
    }

    public static void main(String[] args) {
        long startNanos = System.nanoTime();

        var container = new Container();
        container.scan("generated");
        container.resolveAll();

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        System.out.println("Vessel: %d beans registered, container ready in %dms"
                .formatted(container.registeredTypes().size(), elapsedMillis));
    }
}

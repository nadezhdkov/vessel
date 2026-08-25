package io.vessel.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentReloadTest {

    static class CountingSource implements PropertySource {
        final AtomicInteger refreshCount = new AtomicInteger();

        @Override
        public Optional<String> get(String key) {
            return Optional.empty();
        }

        @Override
        public void refresh() {
            refreshCount.incrementAndGet();
        }
    }

    @Test
    void reloadCallsRefreshOnEverySourceExactlyOnce() {
        var first = new CountingSource();
        var second = new CountingSource();
        var environment = new Environment(List.of(first, second));

        environment.reload();

        assertEquals(1, first.refreshCount.get());
        assertEquals(1, second.refreshCount.get());
    }

    @Test
    void aSourceThatDoesNotOverrideRefreshIsUnaffectedByReload() {
        PropertySource staticSource = key -> Optional.of("constant");
        var environment = new Environment(List.of(staticSource));

        environment.reload();

        assertEquals("constant", environment.require("anything"));
    }
}

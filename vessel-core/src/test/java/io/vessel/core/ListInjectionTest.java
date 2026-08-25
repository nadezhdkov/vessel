package io.vessel.core;

import io.vessel.core.scan.fixtures.listempty.ExtensionHost;
import io.vessel.core.scan.fixtures.listmulti.LoggingPlugin;
import io.vessel.core.scan.fixtures.listmulti.MetricsPlugin;
import io.vessel.core.scan.fixtures.listmulti.PluginHost;
import io.vessel.core.scan.fixtures.listsingle.MetricsCollector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListInjectionTest {

    static class BadListConsumer {
        @SuppressWarnings("rawtypes")
        public BadListConsumer(List items) {
        }
    }

    @Test
    void injectsEveryImplementationOfAnInterfaceAsAList() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.listmulti");

        var host = container.get(PluginHost.class);

        assertEquals(2, host.plugins().size());
        Set<Class<?>> types = host.plugins().stream().map(Object::getClass).collect(Collectors.toSet());
        assertTrue(types.contains(LoggingPlugin.class));
        assertTrue(types.contains(MetricsPlugin.class));
    }

    @Test
    void injectsASingleElementListWhenTheElementTypeHasOnlyOneRegisteredImplementation() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.listsingle");

        var collector = container.get(MetricsCollector.class);

        assertEquals(1, collector.metrics().size());
    }

    @Test
    void injectsAnEmptyListWhenNoImplementationIsRegistered() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.listempty");

        var host = container.get(ExtensionHost.class);

        assertTrue(host.extensions().isEmpty());
    }

    @Test
    void rejectsARawListParameterWithNoConcreteElementType() {
        var container = new Container();
        container.register(BadListConsumer.class);

        var exception = assertThrows(VesselException.class, () -> container.get(BadListConsumer.class));

        assertTrue(exception.getMessage()
                .contains("'BadListConsumer' cannot be registered — parameter 1 is a List with no concrete element type declared"));
    }
}

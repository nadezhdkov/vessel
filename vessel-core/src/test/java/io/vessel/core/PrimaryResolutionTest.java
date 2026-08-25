package io.vessel.core;

import io.vessel.core.scan.fixtures.multipleprimary.Logger;
import io.vessel.core.scan.fixtures.primary.AlertService;
import io.vessel.core.scan.fixtures.primary.PushNotifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimaryResolutionTest {

    @Test
    void usesThePrimaryCandidateWhenNoQualifierIsRequested() {
        var container = new Container();

        container.scan("io.vessel.core.scan.fixtures.primary");

        var alertService = container.get(AlertService.class);

        assertInstanceOf(PushNotifier.class, alertService.notifier());
    }

    @Test
    void throwsWhenMultipleCandidatesAreMarkedPrimary() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.multipleprimary");

        var exception = assertThrows(AmbiguousComponentException.class,
                () -> container.get(Logger.class));

        assertTrue(exception.getMessage()
                .contains("more than one @Component implementing 'Logger' is marked @Primary"));
        assertTrue(exception.getMessage().contains("ConsoleLogger"));
        assertTrue(exception.getMessage().contains("FileLogger"));
    }

    @Test
    void throwsWithAContainerGetRequesterWhenResolvingAnAmbiguousInterfaceDirectly() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.ambiguous");

        var exception = assertThrows(AmbiguousComponentException.class,
                () -> container.get(io.vessel.core.scan.fixtures.ambiguous.Notifier.class));

        assertTrue(exception.getMessage().contains("requested by: container.get(Notifier.class)"));
    }
}

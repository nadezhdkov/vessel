package io.vessel.app;

import io.vessel.app.errorfixtures.brokenwiring.BrokenWiringApplication;
import io.vessel.app.errorfixtures.noapplication.MissingApplicationAnnotation;
import io.vessel.app.errorfixtures.noserver.MissingServerAnnotation;
import io.vessel.core.MissingDependencyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselStartErrorTest {

    @Test
    void startRejectsAClassMissingAtApplication() {
        var exception = assertThrows(VesselAppException.class, () -> Vessel.start(MissingApplicationAnnotation.class));

        assertTrue(exception.getMessage().contains(MissingApplicationAnnotation.class.getName()));
        assertTrue(exception.getMessage().contains("@Application"));
    }

    @Test
    void startRejectsAClassMissingAtServer() {
        var exception = assertThrows(VesselAppException.class, () -> Vessel.start(MissingServerAnnotation.class));

        assertTrue(exception.getMessage().contains(MissingServerAnnotation.class.getName()));
        assertTrue(exception.getMessage().contains("@Server"));
    }

    @Test
    void startFailsAtBootWhenAComponentHasAnUnresolvableDependencyRatherThanOnTheFirstRequest() {
        var exception = assertThrows(MissingDependencyException.class, () -> Vessel.start(BrokenWiringApplication.class));

        assertTrue(exception.getMessage().contains("UnregisteredDependency"));
    }
}

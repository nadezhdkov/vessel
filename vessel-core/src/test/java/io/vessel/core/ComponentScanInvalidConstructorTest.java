package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentScanInvalidConstructorTest {

    @Test
    void propagatesInvalidConstructorExceptionForAScannedComponentWithAmbiguousConstructors() {
        var container = new Container();

        var exception = assertThrows(InvalidConstructorException.class,
                () -> container.scan("io.vessel.core.scan.fixtures.badcomponent"));

        assertTrue(exception.getMessage()
                .contains("'BrokenComponent' cannot be registered — found 2 public constructors"));
    }
}

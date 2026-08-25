package io.vessel.core;

import io.vessel.core.scan.fixtures.simple.GreeterService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentScanTest {

    @Test
    void scanFindsAndRegistersEveryComponentInThePackage() {
        var container = new Container();

        container.scan("io.vessel.core.scan.fixtures.simple");

        var greeter = container.get(GreeterService.class);

        assertEquals("hello", greeter.greet());
    }
}

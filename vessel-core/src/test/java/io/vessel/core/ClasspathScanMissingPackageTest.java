package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathScanMissingPackageTest {

    @Test
    void reportsAClearErrorWhenTheScannedPackageDoesNotExistOnTheClasspath() {
        var container = new Container();

        var exception = assertThrows(VesselException.class,
                () -> container.scan("io.vessel.core.scan.fixtures.does.not.exist"));

        assertTrue(exception.getMessage()
                .contains("no class found in package 'io.vessel.core.scan.fixtures.does.not.exist'"));
    }
}

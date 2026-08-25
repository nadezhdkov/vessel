package io.vessel.core;

import io.vessel.core.scan.fixtures.timed.Greeter;
import io.vessel.core.scan.fixtures.timednointerface.StandaloneTimedComponent;
import io.vessel.core.scan.fixtures.timedwrongmethod.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimedProxyTest {

    private final PrintStream realOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void captureStdOut() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdOut() {
        System.setOut(realOut);
    }

    @Test
    void aTimedMethodStillReturnsTheRealResultAndLogsItsDurationThroughTheProxy() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.timed");

        Greeter greeter = container.get(Greeter.class);
        var result = greeter.greet("Ada");

        assertEquals("Hello, Ada", result);
        assertTrue(capturedOut.toString(StandardCharsets.UTF_8).contains("[TIMED] SlowGreeter.greet took"));
    }

    @Test
    void aNonTimedMethodOnTheSameProxyIsNotLogged() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.timed");

        Greeter greeter = container.get(Greeter.class);
        var result = greeter.shout("ada");

        assertEquals("HELLO, ADA", result);
        assertFalse(capturedOut.toString(StandardCharsets.UTF_8).contains("shout"));
    }

    @Test
    void aTimedMethodOnAClassWithNoInterfaceIsRejectedAtBuildTime() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.timednointerface");

        var exception = assertThrows(InvalidTimedMethodException.class,
                () -> container.get(StandaloneTimedComponent.class));

        assertTrue(exception.getMessage().contains("StandaloneTimedComponent"));
        assertTrue(exception.getMessage().contains("implements no interface"));
    }

    @Test
    void aTimedMethodNotDeclaredOnAnyImplementedInterfaceIsRejectedAtBuildTime() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.timedwrongmethod");

        var exception = assertThrows(InvalidTimedMethodException.class, () -> container.get(Worker.class));

        assertTrue(exception.getMessage().contains("helper"));
        assertTrue(exception.getMessage().contains("WorkerImpl"));
    }
}

package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ContainerRegisterInstanceTest {

    interface Greeter {
        String greet();
    }

    static class EnglishGreeter implements Greeter {
        @Override
        public String greet() {
            return "hello";
        }
    }

    @Test
    void resolvesToTheExactInstancePassedToRegister() {
        var container = new Container();
        var instance = new EnglishGreeter();

        container.register(Greeter.class, instance);

        var resolved = container.get(Greeter.class);

        assertSame(instance, resolved);
    }
}

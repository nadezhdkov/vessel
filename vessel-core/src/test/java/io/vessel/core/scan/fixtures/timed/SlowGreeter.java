package io.vessel.core.scan.fixtures.timed;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Timed;

@Component
public class SlowGreeter implements Greeter {

    public SlowGreeter() {
    }

    @Timed
    @Override
    public String greet(String name) {
        return "Hello, " + name;
    }

    @Override
    public String shout(String name) {
        return "HELLO, " + name.toUpperCase();
    }
}

package io.vessel.app.fixtures;

import io.vessel.core.annotation.Component;

@Component
public class GreetingService {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}

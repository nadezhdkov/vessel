package io.vessel.core.scan.fixtures.simple;

import io.vessel.core.annotation.Component;

@Component
public class GreeterService {

    public String greet() {
        return "hello";
    }
}

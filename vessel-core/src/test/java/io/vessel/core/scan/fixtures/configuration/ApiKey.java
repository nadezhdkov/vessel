package io.vessel.core.scan.fixtures.configuration;

import io.vessel.core.annotation.Component;

@Component
public class ApiKey {

    public String value() {
        return "test-key";
    }
}

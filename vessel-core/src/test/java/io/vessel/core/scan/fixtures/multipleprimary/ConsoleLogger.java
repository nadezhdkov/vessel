package io.vessel.core.scan.fixtures.multipleprimary;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Primary;

@Component
@Primary
public class ConsoleLogger implements Logger {

    @Override
    public void log(String message) {
        // no-op fixture
    }
}

package io.vessel.core.scan.fixtures.primary;

import io.vessel.core.annotation.Component;

@Component
public class EmailNotifier implements Notifier {

    @Override
    public void notify(String message) {
        // no-op fixture
    }
}

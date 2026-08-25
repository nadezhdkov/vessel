package io.vessel.core.scan.fixtures.primary;

import io.vessel.core.annotation.Component;

@Component
public class AlertService {

    private final Notifier notifier;

    public AlertService(Notifier notifier) {
        this.notifier = notifier;
    }

    public Notifier notifier() {
        return notifier;
    }
}

package io.vessel.core.scan.fixtures.primary;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Primary;

@Component
@Primary
public class PushNotifier implements Notifier {

    @Override
    public void notify(String message) {
        // no-op fixture
    }
}

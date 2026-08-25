package io.vessel.core.scan.fixtures.qualified;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Qualifier;

@Component
public class BrokenCheckoutService {

    public BrokenCheckoutService(@Qualifier("nonexistent") PaymentGateway gateway) {
    }
}

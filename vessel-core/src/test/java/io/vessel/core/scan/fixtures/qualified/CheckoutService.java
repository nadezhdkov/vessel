package io.vessel.core.scan.fixtures.qualified;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Qualifier;

@Component
public class CheckoutService {

    private final PaymentGateway gateway;

    public CheckoutService(@Qualifier("stripe") PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public PaymentGateway gateway() {
        return gateway;
    }
}

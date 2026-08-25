package io.vessel.core.scan.fixtures.configuration;

import io.vessel.core.annotation.Component;

@Component
public class CheckoutService {

    private final PaymentGateway gateway;

    public CheckoutService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public PaymentGateway gateway() {
        return gateway;
    }
}

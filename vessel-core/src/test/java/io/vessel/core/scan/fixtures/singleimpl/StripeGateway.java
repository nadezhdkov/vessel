package io.vessel.core.scan.fixtures.singleimpl;

import io.vessel.core.annotation.Component;

@Component
public class StripeGateway implements PaymentGateway {

    @Override
    public String charge(int amountInCents) {
        return "charged " + amountInCents + " via Stripe";
    }
}

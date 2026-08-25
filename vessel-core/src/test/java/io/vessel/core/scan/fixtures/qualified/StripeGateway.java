package io.vessel.core.scan.fixtures.qualified;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Qualifier;

@Component
@Qualifier("stripe")
public class StripeGateway implements PaymentGateway {

    @Override
    public String charge(int amountInCents) {
        return "charged " + amountInCents + " via Stripe";
    }
}

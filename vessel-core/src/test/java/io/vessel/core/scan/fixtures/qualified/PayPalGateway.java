package io.vessel.core.scan.fixtures.qualified;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Qualifier;

@Component
@Qualifier("paypal")
public class PayPalGateway implements PaymentGateway {

    @Override
    public String charge(int amountInCents) {
        return "charged " + amountInCents + " via PayPal";
    }
}

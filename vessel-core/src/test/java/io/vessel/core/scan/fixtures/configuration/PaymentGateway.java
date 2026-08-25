package io.vessel.core.scan.fixtures.configuration;

public interface PaymentGateway {

    String charge(int amountInCents);
}

package io.vessel.core.scan.fixtures.qualified;

public interface PaymentGateway {

    String charge(int amountInCents);
}

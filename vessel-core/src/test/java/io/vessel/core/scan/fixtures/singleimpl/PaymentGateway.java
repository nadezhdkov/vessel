package io.vessel.core.scan.fixtures.singleimpl;

public interface PaymentGateway {

    String charge(int amountInCents);
}

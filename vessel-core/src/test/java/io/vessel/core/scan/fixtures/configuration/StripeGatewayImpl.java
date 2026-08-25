package io.vessel.core.scan.fixtures.configuration;

class StripeGatewayImpl implements PaymentGateway {

    private final String apiKey;

    StripeGatewayImpl(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String charge(int amountInCents) {
        return "charged " + amountInCents + " with key " + apiKey;
    }
}

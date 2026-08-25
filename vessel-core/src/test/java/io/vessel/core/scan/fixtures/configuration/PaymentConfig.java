package io.vessel.core.scan.fixtures.configuration;

import io.vessel.core.annotation.Bean;
import io.vessel.core.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Bean
    public PaymentGateway paymentGateway(ApiKey apiKey) {
        return new StripeGatewayImpl(apiKey.value());
    }
}

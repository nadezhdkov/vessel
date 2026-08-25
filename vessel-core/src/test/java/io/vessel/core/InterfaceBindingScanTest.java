package io.vessel.core;

import io.vessel.core.scan.fixtures.singleimpl.PaymentGateway;
import io.vessel.core.scan.fixtures.singleimpl.StripeGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class InterfaceBindingScanTest {

    @Test
    void resolvesTheInterfaceToItsSoleScannedImplementation() {
        var container = new Container();

        container.scan("io.vessel.core.scan.fixtures.singleimpl");

        var gateway = container.get(PaymentGateway.class);

        assertInstanceOf(StripeGateway.class, gateway);
        assertEquals("charged 500 via Stripe", gateway.charge(500));
    }

    @Test
    void resolvingByInterfaceOrByConcreteTypeReturnsTheSameSingleton() {
        var container = new Container();

        container.scan("io.vessel.core.scan.fixtures.singleimpl");

        var byInterface = container.get(PaymentGateway.class);
        var byConcreteType = container.get(StripeGateway.class);

        assertSame(byInterface, byConcreteType);
    }
}

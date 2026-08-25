package io.vessel.core;

import io.vessel.core.scan.fixtures.qualified.BrokenCheckoutService;
import io.vessel.core.scan.fixtures.qualified.CheckoutService;
import io.vessel.core.scan.fixtures.qualified.StripeGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualifierResolutionTest {

    @Test
    void usesTheQualifierAnnotatedParameterToSelectAmongMultipleImplementations() {
        var container = new Container();

        container.scan("io.vessel.core.scan.fixtures.qualified");

        var checkout = container.get(CheckoutService.class);

        assertInstanceOf(StripeGateway.class, checkout.gateway());
    }

    @Test
    void throwsWhenTheRequestedQualifierMatchesNoCandidate() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.qualified");

        var exception = assertThrows(UnresolvedQualifierException.class,
                () -> container.get(BrokenCheckoutService.class));

        assertTrue(exception.getMessage()
                .contains("no @Component implementing 'PaymentGateway' has @Qualifier(\"nonexistent\")"));
        assertTrue(exception.getMessage().contains("StripeGateway"));
        assertTrue(exception.getMessage().contains("PayPalGateway"));
    }

    @Test
    void throwsWhenMultipleImplementationsExistWithoutAQualifierOrPrimary() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.ambiguous");

        var exception = assertThrows(AmbiguousComponentException.class,
                () -> container.get(io.vessel.core.scan.fixtures.ambiguous.Notifier.class));

        assertTrue(exception.getMessage()
                .contains("more than one @Component implements 'Notifier' and none is marked @Primary"));
        assertTrue(exception.getMessage().contains("@Qualifier(\"name\")"));
    }
}

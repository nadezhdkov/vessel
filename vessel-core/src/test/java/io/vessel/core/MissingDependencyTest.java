package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingDependencyTest {

    static class PaymentGateway {
    }

    static class CheckoutService {
        public CheckoutService(PaymentGateway gateway) {
        }
    }

    @Test
    void reportsTheRootTypeWhenGetIsCalledDirectlyForAnUnregisteredType() {
        var container = new Container();

        var exception = assertThrows(MissingDependencyException.class,
                () -> container.get(PaymentGateway.class));

        assertTrue(exception.getMessage().contains("no bean of type 'PaymentGateway' registered"));
        assertTrue(exception.getMessage().contains("container.get(PaymentGateway.class)"));
    }

    @Test
    void reportsWhoAskedAndTheFullPathWhenTheMissingDependencyIsNested() {
        var container = new Container();
        container.register(CheckoutService.class);

        var exception = assertThrows(MissingDependencyException.class,
                () -> container.get(CheckoutService.class));

        assertTrue(exception.getMessage().contains("no bean of type 'PaymentGateway' registered"));
        assertTrue(exception.getMessage().contains("requested by: CheckoutService(constructor, parameter 1)"));
        assertTrue(exception.getMessage().contains("path: CheckoutService → PaymentGateway"));
    }
}

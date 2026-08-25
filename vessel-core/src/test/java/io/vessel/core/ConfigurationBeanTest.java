package io.vessel.core;

import io.vessel.core.scan.fixtures.configuration.CheckoutService;
import io.vessel.core.scan.fixtures.precedence.Greeter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationBeanTest {

    @Test
    void beanMethodParametersAreResolvedByTheContainerJustLikeConstructorParameters() {
        var container = new Container();

        container.scan("io.vessel.core.scan.fixtures.configuration");

        var checkout = container.get(CheckoutService.class);

        assertEquals("charged 500 with key test-key", checkout.gateway().charge(500));
    }

    @Test
    void beanDefinitionOverridesAComponentForTheSameTypeWithinTheSameScan() {
        var container = new Container();

        container.scan("io.vessel.core.scan.fixtures.precedence");

        var greeter = container.get(Greeter.class);

        assertEquals("from @Bean", greeter.greet());
    }

    @Test
    void rejectsABeanMethodThatReturnsVoid() {
        var container = new Container();

        var exception = assertThrows(InvalidBeanMethodException.class,
                () -> container.scan("io.vessel.core.scan.fixtures.badconfiguration"));

        assertTrue(exception.getMessage()
                .contains("'VoidBeanConfig.broken' cannot be a @Bean — method must return a type, not void"));
    }

    @Test
    void propagatesInvalidConstructorExceptionWhenTheConfigurationClassItselfHasAnAmbiguousConstructor() {
        var container = new Container();

        var exception = assertThrows(InvalidConstructorException.class,
                () -> container.scan("io.vessel.core.scan.fixtures.badconfigctor"));

        assertTrue(exception.getMessage()
                .contains("'BrokenConfig' cannot be registered — found 2 public constructors"));
    }
}

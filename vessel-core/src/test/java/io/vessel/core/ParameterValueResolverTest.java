package io.vessel.core;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterValueResolverTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface FromFixedValue {
        String value();
    }

    static class ServerConfig {
        final int port;
        final String host;

        public ServerConfig(@FromFixedValue("port") int port, String host) {
            this.port = port;
            this.host = host;
        }
    }

    static class BrokenResolver implements ParameterValueResolver {
        @Override
        public boolean supports(Parameter parameter) {
            return parameter.isAnnotationPresent(FromFixedValue.class);
        }

        @Override
        public Object resolve(Parameter parameter) {
            throw new VesselException("simulated external resolution failure");
        }
    }

    private ParameterValueResolver fixedPortResolver(int port) {
        return new ParameterValueResolver() {
            @Override
            public boolean supports(Parameter parameter) {
                return parameter.isAnnotationPresent(FromFixedValue.class);
            }

            @Override
            public Object resolve(Parameter parameter) {
                return port;
            }
        };
    }

    @Test
    void anAnnotatedParameterIsResolvedByThePluggedResolverInsteadOfTheBeanGraph() {
        var container = new Container()
                .withParameterValueResolver(fixedPortResolver(9090))
                .register(String.class, "localhost");
        container.register(ServerConfig.class);

        var config = container.get(ServerConfig.class);

        assertEquals(9090, config.port);
        assertEquals("localhost", config.host);
    }

    @Test
    void withoutAResolverAnAnnotatedParameterFallsBackToNormalGraphResolutionAndFailsAsExpected() {
        var container = new Container().register(ServerConfig.class).register(String.class, "localhost");

        var exception = assertThrows(MissingDependencyException.class, () -> container.get(ServerConfig.class));

        assertTrue(exception.getMessage().contains("int"));
    }

    @Test
    void aFailingExternalResolverPropagatesItsOwnException() {
        var container = new Container()
                .withParameterValueResolver(new BrokenResolver())
                .register(String.class, "localhost");
        container.register(ServerConfig.class);

        var exception = assertThrows(VesselException.class, () -> container.get(ServerConfig.class));

        assertTrue(exception.getMessage().contains("simulated external resolution failure"));
    }
}

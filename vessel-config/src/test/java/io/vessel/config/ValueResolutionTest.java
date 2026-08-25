package io.vessel.config;

import io.vessel.config.annotation.Value;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueResolutionTest {

    static class ServerConfigFixture {
        ServerConfigFixture(@Value("${server.port}") int port) {
        }
    }

    static class MalformedPlaceholderFixture {
        MalformedPlaceholderFixture(@Value("server.port") int port) {
        }
    }

    private Environment environmentOf(Map<String, String> values) {
        return new Environment(List.of(key -> Optional.ofNullable(values.get(key))));
    }

    private Parameter firstParameterOf(Class<?> fixtureClass) throws NoSuchMethodException {
        return fixtureClass.getDeclaredConstructors()[0].getParameters()[0];
    }

    @Test
    void resolvesAnAnnotatedParameterByReadingItsPlaceholderKeyAndConvertingToItsDeclaredType() throws NoSuchMethodException {
        var environment = environmentOf(Map.of("server.port", "8080"));
        var parameter = firstParameterOf(ServerConfigFixture.class);

        assertEquals(8080, environment.resolve(parameter));
    }

    @Test
    void rejectsAPlaceholderMissingTheDollarBraceSyntax() throws NoSuchMethodException {
        var environment = environmentOf(Map.of("server.port", "8080"));
        var parameter = firstParameterOf(MalformedPlaceholderFixture.class);

        var exception = assertThrows(VesselConfigException.class, () -> environment.resolve(parameter));

        assertTrue(exception.getMessage().contains("'server.port'"));
        assertTrue(exception.getMessage().contains("${property.key}"));
    }

    @Test
    void rejectsAPlaceholderWhoseKeyIsNotPresentInAnySource() throws NoSuchMethodException {
        var environment = environmentOf(Map.of());
        var parameter = firstParameterOf(ServerConfigFixture.class);

        var exception = assertThrows(VesselConfigException.class, () -> environment.resolve(parameter));

        assertTrue(exception.getMessage().contains("'server.port'"));
    }

    @Test
    void supportsIsTrueOnlyForParametersCarryingTheValueAnnotation() throws NoSuchMethodException {
        var environment = environmentOf(Map.of());
        var annotated = firstParameterOf(ServerConfigFixture.class);
        var plain = UnannotatedFixture.class.getDeclaredConstructors()[0].getParameters()[0];

        assertTrue(environment.supports(annotated));
        assertFalse(environment.supports(plain));
    }

    static class UnannotatedFixture {
        UnannotatedFixture(int notAnnotated) {
        }
    }

    @Test
    void theAnnotationOnlyTargetsConstructorParametersNeverFields() {
        var target = Value.class.getAnnotation(java.lang.annotation.Target.class);

        assertEquals(1, target.value().length);
        assertEquals(ElementType.PARAMETER, target.value()[0]);
    }

    @Test
    void theAnnotationIsRetainedAtRuntimeSoReflectionCanReadIt() {
        var retention = Value.class.getAnnotation(java.lang.annotation.Retention.class);

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }
}

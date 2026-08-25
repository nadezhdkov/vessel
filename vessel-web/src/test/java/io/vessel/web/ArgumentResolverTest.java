package io.vessel.web;

import io.vessel.http.HttpMethod;
import io.vessel.http.HttpRequest;
import io.vessel.web.annotation.PathVariable;
import io.vessel.web.annotation.RequestBody;
import io.vessel.web.annotation.RequestParam;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentResolverTest {

    record Body(String name) {
    }

    static class Sample {
        void byPathVariable(@PathVariable long id) {
        }

        void byExplicitlyNamedPathVariable(@PathVariable("userId") long id) {
        }

        void byRequestParam(@RequestParam String q) {
        }

        void byRequestBody(@RequestBody Body body) {
        }

        void unannotated(String value) {
        }
    }

    @Test
    void resolvesAPathVariableUsingTheParameterNameWhenNoValueIsGiven() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("byPathVariable", long.class);
        var request = requestWithPathVariables(Map.of("id", "42"));

        var args = ArgumentResolver.resolve(method, request);

        assertArrayEquals(new Object[]{42L}, args);
    }

    @Test
    void resolvesAPathVariableUsingAnExplicitName() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("byExplicitlyNamedPathVariable", long.class);
        var request = requestWithPathVariables(Map.of("userId", "7"));

        var args = ArgumentResolver.resolve(method, request);

        assertArrayEquals(new Object[]{7L}, args);
    }

    @Test
    void throwsAClearErrorWhenThePathVariableIsMissing() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("byPathVariable", long.class);
        var request = requestWithPathVariables(Map.of());

        var exception = assertThrows(VesselWebException.class, () -> ArgumentResolver.resolve(method, request));

        assertTrue(exception.getMessage().contains("no path variable named 'id'"));
    }

    @Test
    void throwsAClearErrorWhenThePathVariableCannotBeConverted() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("byPathVariable", long.class);
        var request = requestWithPathVariables(Map.of("id", "not-a-number"));

        var exception = assertThrows(VesselWebException.class, () -> ArgumentResolver.resolve(method, request));

        assertTrue(exception.getMessage().contains("cannot convert 'id' value 'not-a-number' to long"));
    }

    @Test
    void resolvesARequestParamUsingTheParameterName() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("byRequestParam", String.class);
        var request = new HttpRequest(HttpMethod.GET, "/search", Map.of(), Map.of("q", "vessel"), Map.of(), "");

        var args = ArgumentResolver.resolve(method, request);

        assertArrayEquals(new Object[]{"vessel"}, args);
    }

    @Test
    void throwsAClearErrorWhenTheRequestParamIsMissing() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("byRequestParam", String.class);
        var request = new HttpRequest(HttpMethod.GET, "/search", Map.of(), Map.of(), Map.of(), "");

        var exception = assertThrows(VesselWebException.class, () -> ArgumentResolver.resolve(method, request));

        assertTrue(exception.getMessage().contains("no query parameter named 'q'"));
    }

    @Test
    void resolvesARequestBodyByDeserializingIt() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("byRequestBody", Body.class);
        var request = new HttpRequest(HttpMethod.POST, "/bodies", Map.of(), Map.of(), Map.of(), "{\"name\":\"Alice\"}");

        var args = ArgumentResolver.resolve(method, request);

        assertEquals(new Body("Alice"), args[0]);
    }

    @Test
    void throwsAClearErrorForAnUnannotatedParameter() throws NoSuchMethodException {
        var method = Sample.class.getDeclaredMethod("unannotated", String.class);
        var request = requestWithPathVariables(Map.of());

        var exception = assertThrows(VesselWebException.class, () -> ArgumentResolver.resolve(method, request));

        assertTrue(exception.getMessage().contains("has no resolvable source"));
    }

    private HttpRequest requestWithPathVariables(Map<String, String> pathVariables) {
        return new HttpRequest(HttpMethod.GET, "/x", Map.of(), Map.of(), pathVariables, "");
    }
}

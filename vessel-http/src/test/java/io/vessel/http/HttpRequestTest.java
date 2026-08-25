package io.vessel.http;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestTest {

    @Test
    void headerLookupIsCaseInsensitive() {
        var request = new HttpRequest(HttpMethod.GET, "/users", Map.of("Content-Type", "application/json"),
                Map.of(), Map.of(), "");

        assertEquals("application/json", request.header("content-type").orElseThrow());
        assertEquals("application/json", request.header("CONTENT-TYPE").orElseThrow());
    }

    @Test
    void pathVariablesAreAlwaysEmptyInM4() {
        var request = new HttpRequest(HttpMethod.GET, "/users/42", Map.of(), Map.of(), Map.of(), "");

        assertTrue(request.pathVariables().isEmpty());
    }

    @Test
    void headersMapIsImmutable() {
        var request = new HttpRequest(HttpMethod.GET, "/", Map.of("X-Test", "1"), Map.of(), Map.of(), "");

        assertThrows(UnsupportedOperationException.class, () -> request.headers().put("X-Other", "2"));
    }
}

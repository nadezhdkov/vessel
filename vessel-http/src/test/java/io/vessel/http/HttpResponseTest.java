package io.vessel.http;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpResponseTest {

    @Test
    void okBuildsA200WithUtf8BodyAndContentTypeHeader() {
        var response = HttpResponse.ok("hello");

        assertEquals(HttpStatus.OK, response.status());
        assertEquals("hello", new String(response.body(), StandardCharsets.UTF_8));
        assertEquals("text/plain; charset=utf-8", response.headers().get("Content-Type"));
    }

    @Test
    void noContentHasAnEmptyBody() {
        var response = HttpResponse.noContent();

        assertEquals(HttpStatus.NO_CONTENT, response.status());
        assertArrayEquals(new byte[0], response.body());
    }

    @Test
    void withHeaderOverwritesAnExistingHeaderCaseInsensitively() {
        var response = HttpResponse.ok("hi").withHeader("content-type", "text/html");

        assertEquals(1, response.headers().size());
        assertEquals("text/html", response.headers().get("Content-Type"));
    }

    @Test
    void withHeaderReturnsANewInstanceRatherThanMutating() {
        var original = HttpResponse.ok("hi");
        var withExtra = original.withHeader("X-Trace-Id", "abc123");

        assertEquals(1, original.headers().size());
        assertEquals(2, withExtra.headers().size());
    }
}

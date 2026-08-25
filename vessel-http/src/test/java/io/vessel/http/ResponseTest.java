package io.vessel.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

record User(long id, String name) {
}

class ResponseTest {

    @Test
    void okCarriesA200AndTheBody() {
        var response = Response.ok(new User(1, "Alice"));

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(new User(1, "Alice"), response.body());
    }

    @Test
    void noContentHasA204AndANullBody() {
        var response = Response.noContent();

        assertEquals(HttpStatus.NO_CONTENT, response.status());
        assertNull(response.body());
    }

    @Test
    void statusAllowsAnArbitraryCodeWithABody() {
        var response = Response.status(HttpStatus.NOT_FOUND, "user not found");

        assertEquals(HttpStatus.NOT_FOUND, response.status());
        assertEquals("user not found", response.body());
    }
}

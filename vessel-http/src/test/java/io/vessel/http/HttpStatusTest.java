package io.vessel.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpStatusTest {

    @Test
    void classifiesEachFamilyCorrectly() {
        assertEquals(HttpStatusFamily.SUCCESS, HttpStatus.OK.family());
        assertEquals(HttpStatusFamily.CLIENT_ERROR, HttpStatus.NOT_FOUND.family());
        assertEquals(HttpStatusFamily.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.family());
        assertEquals(HttpStatusFamily.REDIRECTION, HttpStatus.MOVED_PERMANENTLY.family());
        assertEquals(HttpStatusFamily.INFORMATIONAL, HttpStatus.CONTINUE.family());
    }

    @Test
    void isErrorIsTrueForClientAndServerErrorsOnly() {
        assertTrue(HttpStatus.NOT_FOUND.isError());
        assertTrue(HttpStatus.INTERNAL_SERVER_ERROR.isError());
        assertFalse(HttpStatus.OK.isError());
        assertFalse(HttpStatus.MOVED_PERMANENTLY.isError());
    }

    @Test
    void looksUpAKnownCode() {
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.fromCode(404));
        assertEquals(404, HttpStatus.NOT_FOUND.code());
        assertEquals("Not Found", HttpStatus.NOT_FOUND.reason());
    }

    @Test
    void fallsBackToUnknownStatusForAnUnrecognizedCode() {
        assertEquals(HttpStatus.UNKNOWN_STATUS, HttpStatus.fromCode(499));
    }
}

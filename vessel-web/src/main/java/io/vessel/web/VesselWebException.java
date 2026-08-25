package io.vessel.web;

/**
 * Base exception for failures in mapping an HTTP request to a controller
 * method and back — argument resolution, route-to-method wiring, JSON
 * (de)serialization. Distinct from {@code io.vessel.core.VesselException}
 * (DI graph failures) and {@code io.vessel.http.VesselHttpException}
 * (transport-level failures), matching each module's own identity.
 */
public class VesselWebException extends RuntimeException {

    public VesselWebException(String message) {
        super(message);
    }

    public VesselWebException(String message, Throwable cause) {
        super(message, cause);
    }
}

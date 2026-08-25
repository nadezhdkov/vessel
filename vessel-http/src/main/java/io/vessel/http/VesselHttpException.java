package io.vessel.http;

/**
 * Base exception for every failure raised by the Vessel HTTP layer.
 * {@code vessel-http} is a leaf module — it never depends on
 * {@code vessel-core}, so this is deliberately independent from
 * {@code io.vessel.core.VesselException}, not a subtype of it.
 */
public class VesselHttpException extends RuntimeException {

    public VesselHttpException(String message) {
        super(message);
    }

    public VesselHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}

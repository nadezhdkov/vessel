package io.vessel.core;

/**
 * Base exception for every failure raised by the Vessel DI container.
 */
public class VesselException extends RuntimeException {

    public VesselException(String message) {
        super(message);
    }

    public VesselException(String message, Throwable cause) {
        super(message, cause);
    }
}

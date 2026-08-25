package io.vessel.app;

import io.vessel.core.VesselException;

/**
 * Every failure raised while bootstrapping a Vessel application, before the
 * server ever starts accepting requests — a wrong or missing annotation on
 * the class handed to {@link Vessel#start(Class)}, mainly.
 */
public final class VesselAppException extends VesselException {

    public VesselAppException(String message) {
        super(message);
    }

    static VesselAppException missingApplicationAnnotation(Class<?> applicationClass) {
        return new VesselAppException(
                "'%s' cannot be started — it is missing @Application. Vessel.start() needs that annotation on the class to know it is a real entry point, not just any class"
                        .formatted(applicationClass.getName()));
    }

    static VesselAppException missingServerAnnotation(Class<?> applicationClass) {
        return new VesselAppException(
                "'%s' cannot be started — it is missing @Server. Vessel.start() needs at least @Server() (its defaults are fine) to know which port and host to bind"
                        .formatted(applicationClass.getName()));
    }
}

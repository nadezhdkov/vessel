package io.vessel.app.errorfixtures.brokenwiring;

import io.vessel.core.annotation.Component;

@Component
public class BrokenService {
    public BrokenService(UnregisteredDependency dependency) {
    }
}

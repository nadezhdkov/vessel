package io.vessel.core.scan.fixtures.timedwrongmethod;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Timed;

@Component
public class WorkerImpl implements Worker {

    public WorkerImpl() {
    }

    @Override
    public String work() {
        return "worked";
    }

    /** Not on {@link Worker} — a proxy could never intercept this. */
    @Timed
    public String helper() {
        return "helper";
    }
}

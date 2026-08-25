package io.vessel.core.scan.fixtures.timednointerface;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Timed;

@Component
public class StandaloneTimedComponent {

    public StandaloneTimedComponent() {
    }

    @Timed
    public String work() {
        return "done";
    }
}

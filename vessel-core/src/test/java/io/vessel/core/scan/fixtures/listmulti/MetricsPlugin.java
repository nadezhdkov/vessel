package io.vessel.core.scan.fixtures.listmulti;

import io.vessel.core.annotation.Component;

@Component
public class MetricsPlugin implements Plugin {

    @Override
    public String name() {
        return "metrics";
    }
}

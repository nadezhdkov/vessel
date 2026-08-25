package io.vessel.core.scan.fixtures.listsingle;

import io.vessel.core.annotation.Component;

import java.util.List;

@Component
public class MetricsCollector {

    private final List<Metric> metrics;

    public MetricsCollector(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public List<Metric> metrics() {
        return metrics;
    }
}

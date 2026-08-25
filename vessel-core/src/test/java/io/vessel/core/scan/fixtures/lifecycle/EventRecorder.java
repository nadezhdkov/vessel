package io.vessel.core.scan.fixtures.lifecycle;

import io.vessel.core.annotation.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EventRecorder {

    private final List<String> events = new ArrayList<>();

    public void record(String event) {
        events.add(event);
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}

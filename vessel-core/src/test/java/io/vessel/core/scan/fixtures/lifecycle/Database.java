package io.vessel.core.scan.fixtures.lifecycle;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.PostConstruct;
import io.vessel.core.annotation.PreDestroy;

@Component
public class Database {

    private final EventRecorder recorder;

    public Database(EventRecorder recorder) {
        this.recorder = recorder;
    }

    @PostConstruct
    public void connect() {
        recorder.record("Database:postConstruct");
    }

    @PreDestroy
    public void disconnect() {
        recorder.record("Database:preDestroy");
    }
}

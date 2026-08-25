package io.vessel.core.scan.fixtures.lifecycle;

import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.PostConstruct;
import io.vessel.core.annotation.PreDestroy;

@Component
public class Repository {

    private final Database database;
    private final EventRecorder recorder;

    public Repository(Database database, EventRecorder recorder) {
        this.database = database;
        this.recorder = recorder;
    }

    @PostConstruct
    public void init() {
        recorder.record("Repository:postConstruct");
    }

    @PreDestroy
    public void shutdown() {
        recorder.record("Repository:preDestroy");
    }
}

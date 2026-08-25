package io.vessel.app.fixtures;

public class DemoNotFoundException extends RuntimeException {
    public DemoNotFoundException(long id) {
        super("demo item " + id + " not found");
    }
}

package io.vessel.web.fixtures;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("user " + id + " not found");
    }
}

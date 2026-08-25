package io.vessel.web.fixtures;

import io.vessel.http.HttpStatus;
import io.vessel.http.Response;
import io.vessel.web.ErrorResponse;
import io.vessel.web.annotation.ExceptionHandler;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.PathVariable;
import io.vessel.web.annotation.Post;
import io.vessel.web.annotation.RequestBody;
import io.vessel.web.annotation.ResponseStatus;
import io.vessel.web.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    @Get("/users")
    public List<User> users() {
        return List.of(new User(1, "Alice"), new User(2, "Bob"));
    }

    @Get("/users/{id}")
    public Response<User> findUser(@PathVariable long id) {
        if (id == 99) {
            return Response.status(HttpStatus.NOT_FOUND, null);
        }
        return Response.ok(new User(id, "Alice"));
    }

    @Post("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        return user;
    }

    @Get("/users/broken")
    public User broken() {
        throw new IllegalStateException("boom");
    }

    @Get("/users/notfound-demo")
    public User notFoundDemo() {
        throw new UserNotFoundException(42);
    }

    @Get("/users/generic-error")
    public User genericError() {
        throw new RuntimeException("something else entirely");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public Response<ErrorResponse> handleNotFound(UserNotFoundException e) {
        return Response.status(HttpStatus.NOT_FOUND, new ErrorResponse("USER_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Response<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return Response.status(HttpStatus.INTERNAL_SERVER_ERROR, new ErrorResponse("ILLEGAL_STATE", e.getMessage()));
    }

    @ExceptionHandler
    public Response<ErrorResponse> handleGeneric(Exception e) {
        return Response.status(HttpStatus.INTERNAL_SERVER_ERROR, new ErrorResponse("INTERNAL_ERROR", "Unexpected error"));
    }
}

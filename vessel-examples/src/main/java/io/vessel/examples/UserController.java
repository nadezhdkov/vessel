package io.vessel.examples;

import io.vessel.core.annotation.Inject;
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

    private final UserService userService;

    @Inject
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Get("/users")
    public List<User> users() {
        return userService.findAll();
    }

    @Get("/users/{id}")
    public Response<User> getUser(@PathVariable long id) {
        return Response.ok(userService.findById(id));
    }

    @Post("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public Response<ErrorResponse> handleNotFound(UserNotFoundException e) {
        return Response.status(HttpStatus.NOT_FOUND, new ErrorResponse("USER_NOT_FOUND", e.getMessage()));
    }
}

package io.vessel.web.errorfixtures.brokenhandler;

import io.vessel.http.Response;
import io.vessel.web.annotation.ExceptionHandler;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

/** Its own {@code @ExceptionHandler} throws — the last-resort 500 must still win. */
@RestController
public class BrokenHandlerController {

    @Get("/broken-handler/explode")
    public String explode() {
        throw new IllegalStateException("original failure");
    }

    @ExceptionHandler(IllegalStateException.class)
    public Response<String> handle(IllegalStateException e) {
        throw new RuntimeException("the handler itself is broken");
    }
}

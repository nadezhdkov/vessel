package io.vessel.web.errorfixtures.duplicatehandler;

import io.vessel.http.Response;
import io.vessel.web.annotation.ExceptionHandler;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

/** Two handlers registered for the exact same exception type — a scan-time error. */
@RestController
public class DuplicateHandlerController {

    @Get("/duplicate-handler/explode")
    public String explode() {
        throw new IllegalStateException("boom");
    }

    @ExceptionHandler(IllegalStateException.class)
    public Response<String> first(IllegalStateException e) {
        return Response.ok("first");
    }

    @ExceptionHandler(IllegalStateException.class)
    public Response<String> second(IllegalStateException e) {
        return Response.ok("second");
    }
}

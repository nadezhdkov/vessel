package io.vessel.app.fixtures;

import io.vessel.http.HttpStatus;
import io.vessel.http.Response;
import io.vessel.web.annotation.ExceptionHandler;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.PathVariable;
import io.vessel.web.annotation.RestController;

@RestController
public class DemoController {

    private final AppInfo appInfo;

    public DemoController(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    @Get("/greeting")
    public String greeting() {
        return appInfo.greeting();
    }

    @Get("/demo/{id}")
    public String demo(@PathVariable long id) {
        if (id == 99) {
            throw new DemoNotFoundException(id);
        }
        return "demo item " + id;
    }

    @ExceptionHandler(DemoNotFoundException.class)
    public Response<String> handleNotFound(DemoNotFoundException e) {
        return Response.status(HttpStatus.NOT_FOUND, e.getMessage());
    }
}

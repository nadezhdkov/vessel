package io.vessel.web.middlewarefixtures.broken;

import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.web.annotation.After;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

@RestController
public class BrokenAfterController {

    @Get("/broken-after/resource")
    public String resource() {
        return "reached fine";
    }

    @After
    public HttpResponse alwaysFails(HttpRequest request, HttpResponse response) {
        throw new IllegalStateException("after middleware exploded");
    }
}

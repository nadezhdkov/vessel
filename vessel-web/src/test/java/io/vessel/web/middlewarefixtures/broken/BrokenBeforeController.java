package io.vessel.web.middlewarefixtures.broken;

import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.web.annotation.Before;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

@RestController
public class BrokenBeforeController {

    @Get("/broken-before/resource")
    public String resource() {
        return "unreachable";
    }

    @Before
    public HttpResponse alwaysFails(HttpRequest request) {
        throw new IllegalStateException("before middleware exploded");
    }
}

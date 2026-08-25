package io.vessel.web.errorfixtures.duplicatebefore;

import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.web.annotation.Before;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

@RestController
public class DuplicateBeforeController {

    @Get("/duplicate-before/resource")
    public String resource() {
        return "unreachable";
    }

    @Before
    public HttpResponse first(HttpRequest request) {
        return null;
    }

    @Before
    public HttpResponse second(HttpRequest request) {
        return null;
    }
}

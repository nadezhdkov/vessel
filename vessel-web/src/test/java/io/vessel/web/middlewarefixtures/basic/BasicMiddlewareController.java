package io.vessel.web.middlewarefixtures.basic;

import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.web.annotation.After;
import io.vessel.web.annotation.Before;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class BasicMiddlewareController {

    public static final AtomicInteger beforeCallCount = new AtomicInteger();
    public static final AtomicInteger afterCallCount = new AtomicInteger();

    @Get("/basic/hello")
    public String hello() {
        return "hello";
    }

    @Before
    public HttpResponse logRequest(HttpRequest request) {
        beforeCallCount.incrementAndGet();
        return null;
    }

    @After
    public HttpResponse addHeader(HttpRequest request, HttpResponse response) {
        afterCallCount.incrementAndGet();
        return response.withHeader("X-After", "ran");
    }
}

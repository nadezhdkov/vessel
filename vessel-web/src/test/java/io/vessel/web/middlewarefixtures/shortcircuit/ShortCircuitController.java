package io.vessel.web.middlewarefixtures.shortcircuit;

import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.http.HttpStatus;
import io.vessel.web.annotation.After;
import io.vessel.web.annotation.Before;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ShortCircuitController {

    public static final AtomicInteger routeCallCount = new AtomicInteger();
    public static final AtomicInteger afterCallCount = new AtomicInteger();

    @Get("/protected/resource")
    public String resource() {
        routeCallCount.incrementAndGet();
        return "should never be reached";
    }

    @Before
    public HttpResponse requireAuth(HttpRequest request) {
        return HttpResponse.status(HttpStatus.UNAUTHORIZED, "no token provided");
    }

    @After
    public HttpResponse addHeader(HttpRequest request, HttpResponse response) {
        afterCallCount.incrementAndGet();
        return response;
    }
}

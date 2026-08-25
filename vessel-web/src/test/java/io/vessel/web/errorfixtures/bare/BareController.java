package io.vessel.web.errorfixtures.bare;

import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

/** No {@code @ExceptionHandler} at all — exercises the bare 500 safety net. */
@RestController
public class BareController {

    @Get("/bare/explode")
    public String explode() {
        throw new IllegalArgumentException("no handler catches this");
    }
}

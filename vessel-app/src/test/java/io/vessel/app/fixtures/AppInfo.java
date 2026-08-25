package io.vessel.app.fixtures;

import io.vessel.config.annotation.Value;
import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Inject;

/**
 * Mixes {@code @Inject} and {@code @Value} on different parameters of the
 * same constructor. Proves that {@code
 * Container.resolveArguments()} correctly routes {@code greetingService} to
 * the normal bean graph and {@code name}/{@code retries} to the plugged
 * {@code ParameterValueResolver} (the real {@code Environment}), in the same call.
 */
@Component
public class AppInfo {

    private final GreetingService greetingService;
    private final String name;
    private final int retries;

    @Inject
    public AppInfo(GreetingService greetingService, @Value("${app.name}") String name, @Value("${app.retries}") int retries) {
        this.greetingService = greetingService;
        this.name = name;
        this.retries = retries;
    }

    public String greeting() {
        return greetingService.greet(name);
    }

    public int retries() {
        return retries;
    }
}

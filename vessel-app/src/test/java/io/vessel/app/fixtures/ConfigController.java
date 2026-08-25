package io.vessel.app.fixtures;

import io.vessel.config.Environment;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.RestController;

/**
 * Proves {@code Environment} itself is injectable as a normal constructor
 * dependency (not just usable through {@code @Value}) — the M11 wiring in
 * {@code Vessel.start()} that registers it directly into the {@code Container}.
 */
@RestController
public class ConfigController {

    private final Environment environment;

    public ConfigController(Environment environment) {
        this.environment = environment;
    }

    @Get("/config/app-name")
    public String appName() {
        return environment.get("app.name", String.class, "unknown");
    }
}

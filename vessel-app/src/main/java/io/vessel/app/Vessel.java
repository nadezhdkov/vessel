package io.vessel.app;

import io.vessel.app.annotation.Application;
import io.vessel.config.Configuration;
import io.vessel.config.Environment;
import io.vessel.core.Container;
import io.vessel.http.CorsConfig;
import io.vessel.http.VesselHttpServer;
import io.vessel.http.annotation.Server;
import io.vessel.web.ControllerScanner;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The whole bootstrap sequence in one call: read {@code @Server}, load
 * configuration (which can override the annotation's port/host), initialize
 * the DI container, scan
 * for {@code @Component}s and then for {@code @RestController}s (which also
 * registers their {@code @ExceptionHandler}s — see {@link ControllerScanner}),
 * validate the entire dependency graph up front, and only then bind the
 * socket and start accepting requests. A wiring mistake anywhere in that
 * chain surfaces here, at startup — never on the first request that happens
 * to touch the broken part.
 */
public final class Vessel {

    private Vessel() {
    }

    public static VesselHttpServer start(Class<?> applicationClass) {
        Objects.requireNonNull(applicationClass, "applicationClass must not be null");
        long startNanos = System.nanoTime();

        requireAnnotation(applicationClass, Application.class, VesselAppException::missingApplicationAnnotation);
        Server serverAnnotation = requireAnnotation(applicationClass, Server.class, VesselAppException::missingServerAnnotation);

        var environment = Configuration.load();
        int port = environment.get("server.port", Integer.class, serverAnnotation.port());
        String host = environment.get("server.host", String.class, serverAnnotation.host());
        System.out.println("→ @Server detected: port=%d, host=%s, cors=%s"
                .formatted(port, host, serverAnnotation.enableCors() ? "enabled" : "disabled"));
        System.out.println("→ Configuration loaded from application.properties");

        var container = new Container()
                .withParameterValueResolver(environment)
                // registered directly (not scanned) so any @Component/@RestController
                // can @Inject Environment itself, e.g. to call reload() on demand —
                // the M11 "hot-reload without restart" story only works if some
                // running code can reach the live Environment to ask for it.
                .register(Environment.class, environment);
        var server = VesselHttpServer.create(port, host);
        if (serverAnnotation.enableCors()) {
            server.enableCors(CorsConfig.from(serverAnnotation));
        }

        try {
            String basePackage = applicationClass.getPackageName();
            container.scan(basePackage);
            ControllerScanner.scan(basePackage, container, server);
            container.resolveAll();
        } catch (RuntimeException e) {
            // the socket behind VesselHttpServer.create() is already bound at this
            // point (the JDK HttpServer binds on create, not on start) — release it
            // rather than leaking it when startup fails before ever calling start().
            server.stop();
            throw e;
        }
        System.out.println("→ DI Container initialized: %d beans registered".formatted(container.registeredTypes().size()));

        printRoutes(server);
        if (serverAnnotation.enableCors()) {
            System.out.println("→ CORS enabled: origin=%s".formatted(serverAnnotation.corsOrigin()));
        }

        server.start();
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        // server.port(), not the configured `port` — when port=0 asks for an OS-assigned
        // ephemeral port (every test in this module does), `port` itself is still 0 here.
        System.out.println("Vessel started on http://%s:%d in %dms%s"
                .formatted(host, server.port(), elapsedMillis, serverAnnotation.enableCors() ? " (CORS enabled)" : ""));

        return server;
    }

    private static void printRoutes(VesselHttpServer server) {
        var routes = server.routes();
        if (routes.isEmpty()) {
            return;
        }
        System.out.println("→ Routes registered:");
        int methodWidth = routes.stream().mapToInt(route -> route.method().name().length()).max().orElse(0);
        String listing = routes.stream()
                .map(route -> ("    %-" + methodWidth + "s  %s").formatted(route.method(), route.path()))
                .collect(Collectors.joining(System.lineSeparator()));
        System.out.println(listing);
    }

    private static <A extends java.lang.annotation.Annotation> A requireAnnotation(
            Class<?> applicationClass, Class<A> annotationType, java.util.function.Function<Class<?>, VesselAppException> onMissing) {
        A annotation = applicationClass.getAnnotation(annotationType);
        if (annotation == null) {
            throw onMissing.apply(applicationClass);
        }
        return annotation;
    }
}

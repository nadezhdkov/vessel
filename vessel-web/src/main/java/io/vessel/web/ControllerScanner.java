package io.vessel.web;

import io.vessel.core.Container;
import io.vessel.core.scan.ClasspathScanner;
import io.vessel.http.HttpMethod;
import io.vessel.http.Route;
import io.vessel.http.VesselHttpServer;
import io.vessel.web.annotation.Delete;
import io.vessel.web.annotation.Get;
import io.vessel.web.annotation.Post;
import io.vessel.web.annotation.Put;
import io.vessel.web.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Finds {@code @RestController} classes under a base package, registers
 * each directly into the {@link Container} (the "implies @Component"
 * behavior from the annotation's Javadoc), and turns every
 * {@code @Get}/{@code @Post}/{@code @Put}/{@code @Delete} method into a
 * {@link Route} on the given server. Knows nothing about {@code Router}
 * matching itself — that's still entirely {@code Router}'s job from M5;
 * this class only ever produces {@link Route}s for it to consume.
 */
public final class ControllerScanner {

    private ControllerScanner() {
    }

    public static void scan(String basePackage, Container container, VesselHttpServer server) {
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        Objects.requireNonNull(container, "container must not be null");
        Objects.requireNonNull(server, "server must not be null");

        var dispatcher = new Dispatcher(container);

        for (Class<?> candidate : ClasspathScanner.scan(basePackage)) {
            if (!candidate.isAnnotationPresent(RestController.class)) {
                continue;
            }
            container.register(candidate);
            ExceptionResolver.validate(candidate);
            MiddlewareResolver.validate(candidate);
            for (Method method : candidate.getDeclaredMethods()) {
                var mapping = routeMappingOf(method);
                if (mapping == null) {
                    continue;
                }
                method.setAccessible(true);
                server.register(new Route(mapping.httpMethod(), mapping.path(), dispatcher.handlerFor(candidate, method)));
            }
        }
    }

    private static RouteMapping routeMappingOf(Method method) {
        var get = method.getAnnotation(Get.class);
        if (get != null) {
            return new RouteMapping(HttpMethod.GET, get.value());
        }
        var post = method.getAnnotation(Post.class);
        if (post != null) {
            return new RouteMapping(HttpMethod.POST, post.value());
        }
        var put = method.getAnnotation(Put.class);
        if (put != null) {
            return new RouteMapping(HttpMethod.PUT, put.value());
        }
        var delete = method.getAnnotation(Delete.class);
        if (delete != null) {
            return new RouteMapping(HttpMethod.DELETE, delete.value());
        }
        return null;
    }

    private record RouteMapping(HttpMethod httpMethod, String path) {
    }
}

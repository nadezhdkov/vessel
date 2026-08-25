package io.vessel.http.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative server configuration on the application class. {@code
 * vessel-http} itself never reads this annotation — it has no reflection or
 * scanning of its own. Reading a class's {@code @Server} and turning it
 * into a running {@link io.vessel.http.VesselHttpServer} is {@code
 * vessel-app}'s job ({@code Vessel.start()}), which also lets
 * {@code application.properties} override the port/host declared here. See
 * docs/vessel-app.md.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Server {

    int port() default 8080;

    String host() default "localhost";

    boolean enableCors() default false;

    String corsOrigin() default "*";
}

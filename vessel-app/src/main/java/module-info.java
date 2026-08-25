module io.vessel.app {
    requires transitive io.vessel.core;
    requires transitive io.vessel.http;
    requires transitive io.vessel.web;
    requires transitive io.vessel.config;

    exports io.vessel.app;
    exports io.vessel.app.annotation;
}

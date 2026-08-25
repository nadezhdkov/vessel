module io.vessel.web {
    requires transitive io.vessel.core;
    requires transitive io.vessel.http;

    exports io.vessel.web;
    exports io.vessel.web.annotation;
    exports io.vessel.web.json;
}

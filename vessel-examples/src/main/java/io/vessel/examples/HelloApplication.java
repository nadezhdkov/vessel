package io.vessel.examples;

import io.vessel.app.Vessel;
import io.vessel.app.annotation.Application;
import io.vessel.http.annotation.Server;

@Server(port = 8080, host = "localhost")
@Application
public class HelloApplication {

    public static void main(String[] args) {
        Vessel.start(HelloApplication.class);
    }
}

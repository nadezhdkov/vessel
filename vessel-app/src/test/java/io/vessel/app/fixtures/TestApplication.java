package io.vessel.app.fixtures;

import io.vessel.app.annotation.Application;
import io.vessel.http.annotation.Server;

@Server(port = 0, host = "localhost", enableCors = true, corsOrigin = "http://localhost:3000")
@Application
public class TestApplication {
}

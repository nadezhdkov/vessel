package io.vessel.app.errorfixtures.brokenwiring;

import io.vessel.app.annotation.Application;
import io.vessel.http.annotation.Server;

@Server(port = 0)
@Application
public class BrokenWiringApplication {
}

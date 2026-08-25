package io.vessel.core.scan.fixtures.precedence;

import io.vessel.core.annotation.Component;

@Component
public class ComponentGreeter implements Greeter {

    @Override
    public String greet() {
        return "from @Component";
    }
}

package io.vessel.core.scan.fixtures.precedence;

import io.vessel.core.annotation.Bean;
import io.vessel.core.annotation.Configuration;

@Configuration
public class GreeterConfig {

    @Bean
    public Greeter greeter() {
        return () -> "from @Bean";
    }
}

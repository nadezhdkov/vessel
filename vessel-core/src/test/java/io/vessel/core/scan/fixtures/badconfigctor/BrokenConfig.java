package io.vessel.core.scan.fixtures.badconfigctor;

import io.vessel.core.annotation.Bean;
import io.vessel.core.annotation.Configuration;

@Configuration
public class BrokenConfig {

    public BrokenConfig(String name) {
    }

    public BrokenConfig(String name, int age) {
    }

    @Bean
    public String unreachable() {
        return "never registered — constructor resolution fails first";
    }
}

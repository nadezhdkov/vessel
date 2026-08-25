package io.vessel.core.scan.fixtures.badconfiguration;

import io.vessel.core.annotation.Bean;
import io.vessel.core.annotation.Configuration;

@Configuration
public class VoidBeanConfig {

    @Bean
    public void broken() {
    }
}

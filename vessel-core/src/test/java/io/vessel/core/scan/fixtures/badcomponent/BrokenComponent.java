package io.vessel.core.scan.fixtures.badcomponent;

import io.vessel.core.annotation.Component;

@Component
public class BrokenComponent {

    public BrokenComponent(String name) {
    }

    public BrokenComponent(String name, int age) {
    }
}

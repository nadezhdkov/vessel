package io.vessel.core.scan.fixtures.listempty;

import io.vessel.core.annotation.Component;

import java.util.List;

@Component
public class ExtensionHost {

    private final List<Extension> extensions;

    public ExtensionHost(List<Extension> extensions) {
        this.extensions = extensions;
    }

    public List<Extension> extensions() {
        return extensions;
    }
}

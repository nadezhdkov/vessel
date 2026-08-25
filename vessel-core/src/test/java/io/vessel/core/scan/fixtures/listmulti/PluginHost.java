package io.vessel.core.scan.fixtures.listmulti;

import io.vessel.core.annotation.Component;

import java.util.List;

@Component
public class PluginHost {

    private final List<Plugin> plugins;

    public PluginHost(List<Plugin> plugins) {
        this.plugins = plugins;
    }

    public List<Plugin> plugins() {
        return plugins;
    }
}

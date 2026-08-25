package io.vessel.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Assembles the standard {@link Environment}: system properties beat
 * environment variables beat the classpath properties file. Only this class
 * decides that order — {@link Environment} itself is order-agnostic, it just
 * walks whatever list of {@link PropertySource}s it is given.
 */
public final class Configuration {

    private static final String DEFAULT_RESOURCE_NAME = "application.properties";

    private Configuration() {
    }

    public static Environment load() {
        return load(DEFAULT_RESOURCE_NAME);
    }

    public static Environment load(String propertiesResourceName) {
        return new Environment(List.of(
                new SystemPropertySource(),
                new EnvironmentVariableSource(),
                new PropertiesFileSource(propertiesResourceName)));
    }

    /**
     * Reads {@code -Dkey=value} system properties directly — the JVM already
     * uses dotted keys for these, so no key translation is needed.
     */
    private static final class SystemPropertySource implements PropertySource {
        @Override
        public Optional<String> get(String key) {
            return Optional.ofNullable(System.getProperty(key));
        }
    }

    /**
     * Reads OS environment variables using the same relaxed-binding
     * convention Spring Boot popularized: {@code server.port} is looked up
     * as {@code SERVER_PORT}, since shells don't allow dots and don't follow
     * the lowercase conventions property files do — without this
     * translation, this source could never match a key from a properties
     * file at all.
     */
    private static final class EnvironmentVariableSource implements PropertySource {
        @Override
        public Optional<String> get(String key) {
            String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
            return Optional.ofNullable(System.getenv(envKey));
        }
    }

    /**
     * Reads a {@code .properties} file from the classpath. A missing file is
     * not an error — Spring treats {@code application.properties} as
     * optional, and so do we — it just means this source never has a value.
     * Parsed at construction time and cached; {@link #refresh()} (M11)
     * re-reads it, which is what makes {@link Environment#reload()} an
     * actual hot-reload rather than a no-op for this source. Only reflects a
     * genuine change on disk when the resource resolves to a real file on an
     * exploded classpath directory — a resource packaged inside a jar is
     * fixed at build time no matter how many times it's re-read, which is a
     * real limitation worth knowing before reaching for this in a packaged
     * deployment.
     */
    private static final class PropertiesFileSource implements PropertySource {
        private final String resourceName;
        private volatile Properties properties;

        PropertiesFileSource(String resourceName) {
            this.resourceName = resourceName;
            this.properties = load(resourceName);
        }

        @Override
        public Optional<String> get(String key) {
            return Optional.ofNullable(properties.getProperty(key));
        }

        @Override
        public void refresh() {
            this.properties = load(resourceName);
        }

        private static Properties load(String resourceName) {
            var properties = new Properties();
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
                if (stream != null) {
                    properties.load(stream);
                }
            } catch (IOException e) {
                throw VesselConfigException.unreadablePropertiesFile(resourceName, e);
            }
            return properties;
        }
    }
}

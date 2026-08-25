package io.vessel.config;

import java.util.Optional;

/**
 * A pluggable source of raw configuration values. Implementations never
 * convert or validate — that is {@link Environment}'s job — they only answer
 * "do you have a value for this key, verbatim, or not."
 */
public interface PropertySource {
    Optional<String> get(String key);

    /**
     * Re-reads whatever backing state this source caches, if any (M11 —
     * hot-reload of configuration without a restart). The default is a no-op: a
     * source that never caches — {@code SystemPropertySource}/{@code
     * EnvironmentVariableSource} call through to {@code System.getProperty}/
     * {@code System.getenv} on every {@link #get(String)} already — has
     * nothing to refresh. Only a source backed by something read once and
     * cached, like a properties file, needs to override this.
     */
    default void refresh() {
    }
}

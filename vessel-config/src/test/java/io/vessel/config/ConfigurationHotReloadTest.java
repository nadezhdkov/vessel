package io.vessel.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@code refresh()}/{@code reload()} against a real classpath
 * properties file, not a fake {@link PropertySource} — an isolated,
 * throwaway directory added to the context classloader stands in for "a
 * properties file on an exploded classpath", so writing to it between reads
 * is a genuine test of the M11 hot-reload path, not a simulation of one.
 */
class ConfigurationHotReloadTest {

    private ClassLoader originalClassLoader;
    private Path tempDir;
    private Path propertiesFile;

    @BeforeEach
    void setUpIsolatedClasspathDirectory() throws IOException {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        tempDir = Files.createTempDirectory("vessel-hot-reload-test");
        propertiesFile = tempDir.resolve("hot-reload.properties");
        writeProperty("greeting", "hello");

        var isolatedClassLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, originalClassLoader);
        Thread.currentThread().setContextClassLoader(isolatedClassLoader);
    }

    @AfterEach
    void restoreClassLoaderAndCleanUp() throws IOException {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        Files.deleteIfExists(propertiesFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void reloadPicksUpAChangeWrittenToTheFileAfterTheInitialLoad() throws IOException {
        var environment = Configuration.load("hot-reload.properties");
        assertEquals("hello", environment.require("greeting"));

        writeProperty("greeting", "goodbye");
        environment.reload();

        assertEquals("goodbye", environment.require("greeting"));
    }

    @Test
    void withoutCallingReloadTheOriginallyLoadedValueKeepsBeingServed() throws IOException {
        var environment = Configuration.load("hot-reload.properties");
        assertEquals("hello", environment.require("greeting"));

        writeProperty("greeting", "goodbye");

        assertEquals("hello", environment.require("greeting"));
    }

    @Test
    void ifTheFileIsDeletedBeforeAReloadTheKeyGracefullyBecomesAbsentRatherThanThrowing() throws IOException {
        var environment = Configuration.load("hot-reload.properties");
        assertEquals("hello", environment.require("greeting"));

        Files.delete(propertiesFile);
        environment.reload();

        assertTrue(environment.get("greeting").isEmpty());
    }

    private void writeProperty(String key, String value) throws IOException {
        Files.writeString(propertiesFile, key + "=" + value + System.lineSeparator(), StandardCharsets.UTF_8);
    }
}

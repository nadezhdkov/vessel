package io.vessel.core.scan;

import io.vessel.core.VesselException;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Finds every class reachable under a base package on the classpath, walking
 * both exploded directories (the common case in a build's classes dir) and
 * jar entries. Deliberately dumb: it loads every {@code .class} file it
 * finds and lets the caller decide which ones matter (e.g. which carry
 * {@code @Component}) — filtering by annotation is not this class's job.
 */
public final class ClasspathScanner {

    private static final String CLASS_SUFFIX = ".class";

    private ClasspathScanner() {
    }

    public static List<Class<?>> scan(String basePackage) {
        Objects.requireNonNull(basePackage, "base package must not be null");

        String path = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<Class<?>> found = new ArrayList<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                switch (resource.getProtocol()) {
                    case "file" -> scanDirectory(Paths.get(resource.toURI()).toFile(), basePackage, classLoader, found);
                    case "jar" -> scanJar(resource, path, classLoader, found);
                    default -> throw new VesselException(
                            "scan does not support classpath protocol '%s' for package '%s'"
                                    .formatted(resource.getProtocol(), basePackage));
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new VesselException(
                    "failed to scan package '%s': %s".formatted(basePackage, e.getMessage()), e);
        }

        if (found.isEmpty()) {
            throw new VesselException(
                    "no class found in package '%s' — check that the package exists on the classpath and is not empty"
                            .formatted(basePackage));
        }

        return found;
    }

    private static void scanDirectory(File directory, String packageName, ClassLoader classLoader, List<Class<?>> found) {
        File[] entries = directory.listFiles();
        if (entries == null) {
            return;
        }

        for (File entry : entries) {
            if (entry.isDirectory()) {
                scanDirectory(entry, packageName + "." + entry.getName(), classLoader, found);
            } else if (entry.getName().endsWith(CLASS_SUFFIX) && !isSyntheticPackageClass(entry.getName())) {
                var simpleName = entry.getName().substring(0, entry.getName().length() - CLASS_SUFFIX.length());
                loadClass(packageName + "." + simpleName, classLoader, found);
            }
        }
    }

    private static void scanJar(URL resource, String path, ClassLoader classLoader, List<Class<?>> found) throws IOException {
        var connection = (JarURLConnection) resource.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(path) && name.endsWith(CLASS_SUFFIX)
                        && !isSyntheticPackageClass(name)) {
                    var className = name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.');
                    loadClass(className, classLoader, found);
                }
            }
        }
    }

    private static boolean isSyntheticPackageClass(String fileName) {
        return fileName.equals("package-info.class") || fileName.equals("module-info.class");
    }

    private static void loadClass(String fullyQualifiedName, ClassLoader classLoader, List<Class<?>> found) {
        try {
            found.add(Class.forName(fullyQualifiedName, false, classLoader));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            throw new VesselException(
                    "failed to load class '%s' during scan: %s".formatted(fullyQualifiedName, e.getMessage()), e);
        }
    }
}

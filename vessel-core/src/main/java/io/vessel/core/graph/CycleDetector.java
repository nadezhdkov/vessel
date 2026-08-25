package io.vessel.core.graph;

import io.vessel.core.CircularDependencyException;

import java.util.Deque;

/**
 * Checks whether resolving {@code candidate} would revisit a type already
 * present in the current resolution path.
 */
public final class CycleDetector {

    private CycleDetector() {
    }

    public static void checkForCycle(Deque<Class<?>> path, Class<?> candidate) {
        if (path.contains(candidate)) {
            throw CircularDependencyException.forCycle(path, candidate);
        }
    }
}

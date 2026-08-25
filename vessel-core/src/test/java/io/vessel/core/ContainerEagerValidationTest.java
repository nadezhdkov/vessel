package io.vessel.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerEagerValidationTest {

    static class Repository {
        public Repository() {
        }
    }

    static class Service {
        public Service(Repository repository) {
        }
    }

    static final AtomicInteger constructionCount = new AtomicInteger();

    static class Counted {
        public Counted() {
            constructionCount.incrementAndGet();
        }
    }

    @Test
    void registeredTypesReflectsEveryDirectRegistrationRegardlessOfResolutionOrder() {
        var container = new Container().register(Repository.class).register(Service.class);

        assertEquals(2, container.registeredTypes().size());
        assertTrue(container.registeredTypes().contains(Repository.class));
        assertTrue(container.registeredTypes().contains(Service.class));
    }

    @Test
    void resolveAllConstructsEveryRegisteredTypeUpFrontWithoutAnyGetCall() {
        constructionCount.set(0);
        var container = new Container().register(Counted.class);

        container.resolveAll();

        assertEquals(1, constructionCount.get());
    }

    @Test
    void resolveAllSurfacesAWiringMistakeImmediatelyInsteadOfOnTheFirstRealRequest() {
        var container = new Container().register(Repository.class);
        container.register(BrokenServiceWithMissingDependency.class);

        var exception = assertThrows(MissingDependencyException.class, container::resolveAll);

        assertTrue(exception.getMessage().contains("SomeUnregisteredType"));
    }

    static class SomeUnregisteredType {
    }

    static class BrokenServiceWithMissingDependency {
        public BrokenServiceWithMissingDependency(SomeUnregisteredType dependency) {
        }
    }
}

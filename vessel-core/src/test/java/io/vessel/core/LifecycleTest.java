package io.vessel.core;

import io.vessel.core.annotation.PostConstruct;
import io.vessel.core.annotation.PreDestroy;
import io.vessel.core.scan.fixtures.lifecycle.EventRecorder;
import io.vessel.core.scan.fixtures.lifecycle.Repository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleTest {

    static class TwoPostConstructs {
        public TwoPostConstructs() {
        }

        @PostConstruct
        public void first() {
        }

        @PostConstruct
        public void second() {
        }
    }

    static class BadSignaturePostConstruct {
        public BadSignaturePostConstruct() {
        }

        @PostConstruct
        public String init() {
            return "not void";
        }
    }

    static class TwoPreDestroys {
        public TwoPreDestroys() {
        }

        @PreDestroy
        public void first() {
        }

        @PreDestroy
        public void second() {
        }
    }

    static class BadSignaturePreDestroy {
        public BadSignaturePreDestroy() {
        }

        @PreDestroy
        public void shutdown(String reason) {
        }
    }

    @Test
    void postConstructRunsForEachDependencyBeforeItsDependentAndPreDestroyRunsInReverseCreationOrder() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.lifecycle");

        container.get(Repository.class);
        var recorder = container.get(EventRecorder.class);

        assertEquals(List.of("Database:postConstruct", "Repository:postConstruct"), recorder.events());

        container.close();

        assertEquals(
                List.of("Database:postConstruct", "Repository:postConstruct",
                        "Repository:preDestroy", "Database:preDestroy"),
                recorder.events());
    }

    @Test
    void rejectsAClassWithMoreThanOnePostConstructMethod() {
        var container = new Container();
        container.register(TwoPostConstructs.class);

        var exception = assertThrows(InvalidLifecycleMethodException.class,
                () -> container.get(TwoPostConstructs.class));

        assertTrue(exception.getMessage()
                .contains("'TwoPostConstructs' cannot be used — found 2 methods annotated with @PostConstruct"));
    }

    @Test
    void rejectsAPostConstructMethodThatIsNotVoidOrTakesParameters() {
        var container = new Container();
        container.register(BadSignaturePostConstruct.class);

        var exception = assertThrows(InvalidLifecycleMethodException.class,
                () -> container.get(BadSignaturePostConstruct.class));

        assertTrue(exception.getMessage()
                .contains("'BadSignaturePostConstruct' cannot be used — method 'init' annotated with @PostConstruct must be void and take no parameters"));
    }

    @Test
    void rejectsAClassWithMoreThanOnePreDestroyMethod() {
        var container = new Container();
        container.register(TwoPreDestroys.class);
        container.get(TwoPreDestroys.class);

        var exception = assertThrows(InvalidLifecycleMethodException.class, container::close);

        assertTrue(exception.getMessage()
                .contains("'TwoPreDestroys' cannot be used — found 2 methods annotated with @PreDestroy"));
    }

    @Test
    void rejectsAPreDestroyMethodThatTakesParameters() {
        var container = new Container();
        container.register(BadSignaturePreDestroy.class);
        container.get(BadSignaturePreDestroy.class);

        var exception = assertThrows(InvalidLifecycleMethodException.class, container::close);

        assertTrue(exception.getMessage()
                .contains("'BadSignaturePreDestroy' cannot be used — method 'shutdown' annotated with @PreDestroy must be void and take no parameters"));
    }
}

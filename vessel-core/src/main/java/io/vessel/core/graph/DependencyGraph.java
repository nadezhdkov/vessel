package io.vessel.core.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Plain registry of {@link BeanDefinition}s, keyed by type, plus the set of
 * concrete classes {@link io.vessel.core.Container#scan(String)} found
 * implementing each interface. Knows nothing about resolution order,
 * singletons, qualifiers, or cycles — that logic belongs to the container.
 */
public final class DependencyGraph {

    private final Map<Class<?>, BeanDefinition> definitions = new LinkedHashMap<>();
    private final Map<Class<?>, List<Class<?>>> interfaceCandidates = new LinkedHashMap<>();

    public void register(BeanDefinition definition) {
        definitions.put(definition.type(), definition);
    }

    public Optional<BeanDefinition> definitionOf(Class<?> type) {
        return Optional.ofNullable(definitions.get(type));
    }

    public boolean isRegistered(Class<?> type) {
        return definitions.containsKey(type);
    }

    public void bindInterface(Class<?> iface, Class<?> implementation) {
        interfaceCandidates.computeIfAbsent(iface, unused -> new ArrayList<>()).add(implementation);
    }

    public List<Class<?>> candidatesFor(Class<?> iface) {
        return interfaceCandidates.getOrDefault(iface, List.of());
    }

    public Set<Class<?>> registeredTypes() {
        return Set.copyOf(definitions.keySet());
    }
}

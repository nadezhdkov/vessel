package io.vessel.core;

import java.lang.reflect.Parameter;

/**
 * Extension point for resolving a constructor parameter's value from
 * somewhere other than the bean graph — the reason it exists is
 * {@code vessel-config}'s {@code @Value}, which must pull its value from an
 * {@code Environment} rather than from another registered bean. {@code
 * vessel-core} is a leaf module and can never import {@code vessel-config}
 * (see CLAUDE.md's dependency-graph rule), so this interface is how a
 * higher module plugs into constructor resolution without that dependency
 * ever existing: {@code vessel-core} only knows the shape of "something that
 * can answer for a parameter," never the {@code @Value} annotation itself.
 */
public interface ParameterValueResolver {

    boolean supports(Parameter parameter);

    Object resolve(Parameter parameter);
}

package io.vessel.core.annotation;

/**
 * The two lifecycles {@link Scope} can select. There is no custom/pluggable
 * scope in Vessel — a request-scoped bean or similar would need a caller to
 * be inside some kind of active request context, which nothing in this
 * framework tracks; without a concrete use case driving the design, adding
 * that machinery isn't worth the complexity.
 */
public enum ScopeType {
    SINGLETON,
    PROTOTYPE
}

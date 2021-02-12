/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.api.threadcontext;

/**
 * Builder for immutable {@link ThreadContext} instances.
 * 
 * @author Robert Mischke
 */
public final class ThreadContextBuilder {

    private ThreadContext currentContextHead;

    /**
     * Internal {@link ThreadContext} implementation representing the empty "root" context. Implemented as a separate class to avoid
     * frequent "parent == null" checks during aspect lookup.
     * 
     * @author Robert Mischke
     */
    private static final class ImmutableRootThreadContextImpl implements ThreadContext {

        @Override
        public <T> T getAspect(Class<T> aspectType) {
            return null;
        }
    }

    /**
     * Internal {@link ThreadContext} implementation. Instead of a Map, which would require frequent copying on modification, this is
     * implemented as a backwards-linked list of immutable elements. Lookup is performed by simple linear backwards search, which should
     * also be more efficient for the low expected number of aspects per context.
     * 
     * @author Robert Mischke
     */
    private static final class ImmutableThreadContextImpl implements ThreadContext {

        private final ThreadContext parentContext;

        private final Object aspectKey;

        private final Object aspectValue;

        /**
         * @param inputMap the map to wrap into this {@link ThreadContext}; note that for efficiency, this map is NOT copied but referenced
         *        - therefore, it MUST NOT be modified externally after it has been passed to this constructor
         */
        <T> ImmutableThreadContextImpl(ThreadContext parentContext, Class<T> aspectKey, T aspectValue) {
            this.parentContext = parentContext;
            this.aspectKey = aspectKey;
            this.aspectValue = aspectValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAspect(Class<T> aspectType) {
            if (aspectType == this.aspectKey) { // note: expects key types to be instantiated by the same classloader
                return (T) aspectValue;
            } else {
                return parentContext.getAspect(aspectType);
            }
        }

        @Override
        public String toString() {
            ThreadContextNameProvider nameProvider = getAspect(ThreadContextNameProvider.class);
            if (nameProvider != null) {
                return nameProvider.getName(this);
            } else {
                return super.toString(); // fallback
            }
        }

    }

    private ThreadContextBuilder(ThreadContext parent) {
        this.currentContextHead = parent;
    }

    /**
     * @return a new builder containing no aspects
     */
    public static ThreadContextBuilder empty() {
        return new ThreadContextBuilder(new ImmutableRootThreadContextImpl());
    }

    /**
     * @param parentContext the context to inherit all aspects from
     * @return a new builder containing all aspects of the provided context
     */
    public static ThreadContextBuilder from(ThreadContext parentContext) {
        return new ThreadContextBuilder(parentContext);
    }

    /**
     * @return a new builder inheriting all aspects of the current thread's context; delegates to {@link #empty()} if the current context is
     *         null
     */
    public static ThreadContextBuilder fromCurrent() {
        final ThreadContext currentContext = ThreadContextHolder.getCurrentContext();
        if (currentContext != null) {
            return from(currentContext);
        } else {
            return empty(); // creates the required "root" parent
        }
    }

    /**
     * Registers an "aspect" - an arbitrary object matching the provided key type.
     * 
     * @param <T> the type (usually an interface) to register the aspect object under
     * @param key the type (usually an interface) to register the aspect object under
     * @param value the aspect object to register
     * @return the builder (for call chaining)
     */
    public <T> ThreadContextBuilder setAspect(Class<T> key, T value) {
        currentContextHead = new ImmutableThreadContextImpl(currentContextHead, key, value);
        return this;
    }

    /**
     * Sets a static name for the {@link ThreadContext} under construction. If the name contains dynamic parts depending on context data,
     * consider using {@link #setNameProvider(ThreadContextNameProvider)} and generating the name on the fly instead.
     * 
     * @param staticName the name to set
     * @return the builder (for call chaining)
     */
    public ThreadContextBuilder setName(String staticName) {
        setAspect(ThreadContextNameProvider.class, new ThreadContextStaticNameProvider(staticName));
        return this;
    }

    /**
     * Sets a dynamic name provider for the {@link ThreadContext} under construction. It is a typical use case to set a shared
     * {@link ThreadContextNameProvider} for similar {@link ThreadContext}s, and have the provider generate the specific name for each
     * context on the fly.
     * 
     * @param nameProvider a dynamic generator for names of {@link ThreadContext}s
     * @return the builder (for call chaining)
     */
    public ThreadContextBuilder setNameProvider(ThreadContextNameProvider nameProvider) {
        setAspect(ThreadContextNameProvider.class, nameProvider);
        return this;
    }

    /**
     * Wraps the current builder state into an immutable {@link ThreadContext}.
     * 
     * @return the generated {@link ThreadContext}
     */
    public ThreadContext build() {
        return currentContextHead;
    }

}

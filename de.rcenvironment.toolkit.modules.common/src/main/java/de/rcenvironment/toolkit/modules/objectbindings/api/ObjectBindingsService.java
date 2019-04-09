/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.objectbindings.api;

/**
 * A service providing a registry of object "bindings" for a class, and consumers consuming these bindings. Bindings are simply instances of
 * the binding class (for example, a listener interface). There can be multiple instances for the same class.
 * <p>
 * A typical use case would be a service providing a listener interface for notifications about its state. This service would register
 * itself as a consumer for the listener interface class, and any listener would register itself as a binding for the listener interface.
 * The service will then receive all listener implementations (regardless of whether they were registered before or after the service) via
 * {@link ObjectBindingsConsumer#addInstance(Object)}.
 * <p>
 * The advantages of this service over direct listener registration are:
 * 
 * <ul>
 * <li>less coupling between the participants of the binding (implementation/consumer)
 * <li>bindings/listeners can always be registered regardless of the consumer's lifecycle
 * <li>the ability to replace the consumer for a certain binding class while maintaining the bindings setup
 * <li>better support for centralized deregistration (via {@link #removeAllBindingsOfOwner(Object)}
 * </ul>
 * .
 * 
 * @author Robert Mischke
 * 
 */
public interface ObjectBindingsService {

    /**
     * @param <T> see "bindingClass" parameter
     * @param bindingClass the "binding class"; usually a service or listener interface that allows multiple instances at once to be
     *        registered
     * @param implementation the binding class implementation to register
     * @param owner the "owner" of this binding, which is typically the object performing the {@link #addBinding()} call; can be used to
     *        simplify deregistration of listeners or plugins by simply calling {@link #removeAllBindingsOfOwner(this)} on shutdown
     */
    <T> void addBinding(Class<T> bindingClass, T implementation, Object owner);

    /**
     * @param <T> see "bindingClass" parameter
     * @param bindingClass the "binding class"; usually a service or listener interface that allows multiple instances at once to be
     *        registered
     * @param implementation the binding class implementation to register
     */
    <T> void removeBinding(Class<T> bindingClass, T implementation);

    /**
     * NOTE: this method is not implemented yet.
     * 
     * @param owner the "owner" for which all registered bindings should be removed/unregistered
     */
    void removeAllBindingsOfOwner(Object owner);

    /**
     * Sets the consumer for a certain binding class. All registered instances (or their removal) for a binding class will be reported to
     * its registered consumer, regardless or whether the binding or the consumer was added first. Similarly, unregistered bindings will be
     * deregistered from a consumer, and the same happens when the consumer itself is deregistered.
     * 
     * @param <T> see "bindingClass" parameter
     * @param bindingClass the "binding class"; usually a service or listener interface that allows multiple instances at once to be
     *        registered
     * @param consumer the consumer to which all registered bindings are reported
     */
    <T> void setConsumer(Class<T> bindingClass, ObjectBindingsConsumer<T> consumer);
}

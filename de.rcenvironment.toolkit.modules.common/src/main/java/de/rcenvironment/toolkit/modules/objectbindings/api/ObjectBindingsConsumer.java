/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.objectbindings.api;

/**
 * A listener interface that synchronizes the consumer with the set of registered binding instances (ie, implementations of a
 * "binding class"; usually a service or listener interface).
 * 
 * @author Robert Mischke
 * 
 * @param <T> the binding class
 */
public interface ObjectBindingsConsumer<T> {

    /**
     * A callback that is invoked when
     * 
     * <ul>
     * <li>this binding instance (the parameter) was added after this consumer implementation (the callback receiver) was registered
     * <li>this consumer implementation (the callback receiver) was registered after this binding instance (the parameter) was added
     * </ul>
     * .
     * 
     * @param instance the instance (an implementation of a binding class)
     */
    void addInstance(T instance);

    /**
     * A callback that is invoked when
     * 
     * <ul>
     * <li>this binding instance (the parameter) was removed after this consumer implementation (the callback receiver) was registered
     * <li>this consumer implementation (the callback receiver) was unregistered after this binding instance (the parameter) was added
     * </ul>
     * .
     * 
     * @param instance the instance (an implementation of a binding class)
     */
    void removeInstance(T instance);
}

/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.spi.module;

import de.rcenvironment.toolkit.core.api.Toolkit;

/**
 * A simple boolean filter for {@link Class} instances. In the context of the {@link Toolkit}, this is used to determine which of the
 * interfaces implemented by a class should be exported as a service interface.
 * 
 * @author Robert Mischke
 */
public interface ClassFilter {

    /**
     * Decider method.
     * 
     * @param clazz the class to inspect
     * @return true if the class should be accepted/kept, false to filter/remove/ignore it.
     */
    boolean accept(Class<?> clazz);
}

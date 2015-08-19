/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.util.Collection;

/**
 * An interface for OSGi-DS services that allows them to register listener services without
 * subclassing their interfaces (see Mantis #9423).
 * 
 * @author Robert Mischke
 */
public interface ListenerProvider {

    /**
     * @return the {@link ListenerDeclaration}s that define the listeners that the provider wants to
     *         register
     */
    // TODO naming: "define" vs. "declare"?
    Collection<ListenerDeclaration> defineListeners();
}

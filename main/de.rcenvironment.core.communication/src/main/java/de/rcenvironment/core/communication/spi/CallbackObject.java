/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.spi;

import java.io.Serializable;

/**
 * Interface which needs to be implemented by objects which needs to be calles back from a remote
 * platform.
 * 
 * @author Doreen Seider
 */
public interface CallbackObject extends Serializable {

    /**
     * Returns the interface the object is implementing. It is used for creating a {@link Proxy}.
     * 
     * @return The interface the object is implementing.
     */
    Class<?> getInterface();
}

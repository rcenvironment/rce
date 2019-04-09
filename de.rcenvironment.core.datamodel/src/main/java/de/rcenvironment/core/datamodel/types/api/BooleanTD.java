/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * A container for a boolean value.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface BooleanTD extends TypedDatum {

    /**
     * @return the boolean value of this {@link TypedDatum}
     */
    boolean getBooleanValue();
}

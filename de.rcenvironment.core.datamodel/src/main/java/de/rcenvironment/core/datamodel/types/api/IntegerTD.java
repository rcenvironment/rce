/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * A container for a 64 bit signed integer.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface IntegerTD extends TypedDatum {

    /**
     * @return int value of this {@link TypedDatum}
     */
    long getIntValue();
}

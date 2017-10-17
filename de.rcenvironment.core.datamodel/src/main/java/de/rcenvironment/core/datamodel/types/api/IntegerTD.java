/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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

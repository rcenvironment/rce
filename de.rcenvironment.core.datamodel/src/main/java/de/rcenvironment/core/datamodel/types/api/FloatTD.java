/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * A container for a double-precision (64 bit) float.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface FloatTD extends TypedDatum {

    /**
     * @return the double-precision (64 bit) float value of this {@link TypedDatum}
     */
    double getFloatValue();

}

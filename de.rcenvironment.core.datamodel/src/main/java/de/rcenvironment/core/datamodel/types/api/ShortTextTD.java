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
 * A string of limited length. The rationale for the length restriction is to avoid RAM shortage
 * from handling arbitrary-sized data as strings. Large string data should be handled via
 * {@link #FileReference} instead, using temporary files or direct data management upload if needed.
 * 
 * Maximum character: 140
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface ShortTextTD extends TypedDatum {

    /**
     * @return short text value of this {@link TypedDatum}
     */
    String getShortTextValue();
    
    /**
     * @param maxLength maximum length of string representation
     * @return string representation
     */
    String toLengthLimitedString(int maxLength);
}

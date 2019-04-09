/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import java.util.Date;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * A container for a timezone-independent timestamp.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface DateTimeTD extends TypedDatum {

    /**
     * @return date and time
     */
    Date getDateTime();

    /**
     * @return date and time in milliseconds since Jan 1, 1970
     */
    long getDateTimeInMilliseconds();
}

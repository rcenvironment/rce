/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;


/**
 * The base interface for all typed datum instances. These are the basic units of data passed
 * between workflow components.
 * 
 * @author Robert Mischke
 */
public interface TypedDatum {

    /**
     * @return the runtime {@link DataType} of the {@link TypedDatum}
     */
    DataType getDataType();

}

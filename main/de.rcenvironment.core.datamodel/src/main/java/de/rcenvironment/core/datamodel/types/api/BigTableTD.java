/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * A table of arbitrary size that is partially loaded into RAM on access. Each cell has its
 * individual data type. Valid cell data types are defined by the {@link #isAllowedAsCellType()}
 * method.
 * 
 * @author Robert Mischke
 */
public interface BigTableTD extends TypedDatum {

}

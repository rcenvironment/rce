/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

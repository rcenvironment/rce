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
 * A container for an empty value.
 * 
 * @author Doreen Seider
 */
public interface NotAValueTD extends TypedDatum {

    /**
     * @return identifier of the instantiated {@link NotAValueTD}. Used to identify transfer cycles of the {@link NotAValueTD}.
     */
    String getIdentifier();
}

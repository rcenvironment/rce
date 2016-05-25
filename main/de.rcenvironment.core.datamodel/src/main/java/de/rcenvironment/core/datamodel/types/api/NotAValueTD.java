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
 * A container for an empty value.
 * 
 * @author Doreen Seider
 */
public interface NotAValueTD extends TypedDatum {
    
    /** Suffix used to indicate whether {@link NotAValueTD} was caused by a component failure. (Will be removed in 8.0.) */
    String FAILURE_CAUSE_SUFFIX = "_flr";

    /**
     * Cause why {@link NotAValueTD} was sent.
     * 
     * @author Doreen Seider
     */
    enum Cause {
        InvalidInputs,
        Failure
    }
    /**
     * @return identifier of the instantiated {@link NotAValueTD}. Used to identify transfer cycles of the {@link NotAValueTD}.
     */
    String getIdentifier();
    
    /**
     * @return the cause why {@link NotAValueTD} was sent
     */
    Cause getCause();
    
    
}

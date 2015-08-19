/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.EmptyTD;

/**
 * Implementation of {@link EmptyTD}.
 * 
 * @author Doreen Seider
 */
public class EmptyTDImpl extends AbstractTypedDatum implements EmptyTD {

    public EmptyTDImpl() {
        super(DataType.Empty);
    }

    @Override
    public String toString() {
        return "nil";
    }

}

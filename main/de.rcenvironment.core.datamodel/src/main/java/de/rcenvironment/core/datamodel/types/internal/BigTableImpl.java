/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.BigTableTD;

/**
 * Implementation of {@link BigTableTD}.
 * 
 * @author Robert Mischke
 */
public class BigTableImpl extends AbstractTypedDatum implements BigTableTD {

    public BigTableImpl() {
        super(DataType.BigTable);
    }

}

/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.internal;

import java.util.UUID;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;

/**
 * Implementation of {@link NotAValueTD}.
 * 
 * @author Doreen Seider
 */
public class NotAValueTDImpl extends AbstractTypedDatum implements NotAValueTD {

    private final String identifier;
    
    public NotAValueTDImpl() {
        this(UUID.randomUUID().toString());
    }
    
    public NotAValueTDImpl(String identifier) {
        super(DataType.NotAValue);
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "./.";
    }
    
}

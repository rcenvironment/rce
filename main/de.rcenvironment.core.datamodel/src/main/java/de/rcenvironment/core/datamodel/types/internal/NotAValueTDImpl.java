/*
 * Copyright (C) 2006-2016 DLR, Germany
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
    
    private final Cause cause;
    
    public NotAValueTDImpl(Cause cause) {
        this(UUID.randomUUID().toString(), cause);
    }
    
    public NotAValueTDImpl(String identifier, Cause cause) {
        super(DataType.NotAValue);
        this.identifier = identifier;
        this.cause = cause;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
    
    @Override
    public Cause getCause() {
        return cause;
    }
    
    @Override
    public String toString() {
        return "./.";
    }

}

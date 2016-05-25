/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.testutils;

import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.datamodel.types.internal.NotAValueTDImpl;

/**
 * Default mock for {@link NotAValueTD}. All methods behave like {@link NotAValueTDImpl}.
 * 
 * @author Doreen Seider
 */
public class NotAValueTDStub extends NotAValueTDImpl {

    public NotAValueTDStub(String id) {
        super(id, NotAValueTD.Cause.InvalidInputs);
    }
    
    public NotAValueTDStub() {
        super(NotAValueTD.Cause.InvalidInputs);
    }
    

}

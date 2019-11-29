/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

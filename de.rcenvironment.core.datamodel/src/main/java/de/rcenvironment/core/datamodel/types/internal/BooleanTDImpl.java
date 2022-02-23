/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.types.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;

/**
 * Implementation of {@link BooleanTD}.
 * 
 * @author Doreen Seider
 */
public class BooleanTDImpl extends AbstractTypedDatum implements BooleanTD {

    private final boolean booleanValue;
    
    public BooleanTDImpl(boolean booleanValue) {
        super(DataType.Boolean);
        this.booleanValue = booleanValue;
    }

    @Override
    public boolean getBooleanValue() {
        return booleanValue;
    }
    
    @Override
    public int hashCode() {
        final int randomDigit1 = 1231;
        final int randomDigit2 = 1237;
        final int prime = 31;
        int result = 1;
        if (booleanValue) {
            result = prime * result + randomDigit1;
        } else {
            result = prime * result + randomDigit2;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof BooleanTD) {
            BooleanTD other = (BooleanTD) obj;
            return booleanValue == other.getBooleanValue();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(getBooleanValue());
    }

}

/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.types.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;

/**
 * Implementation of {@link IntegerTD}.
 * 
 * @author Doreen Seider
 */
public class IntegerTDImpl extends AbstractTypedDatum implements IntegerTD {

    private final long intValue;

    public IntegerTDImpl(long intValue) {
        super(DataType.Integer);
        this.intValue = intValue;
    }
    
    @Override
    public long getIntValue() {
        return intValue;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        final int randomDigit = 32;
        int result = 1;
        result = prime * result + (int) (intValue ^ (intValue >>> randomDigit));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof IntegerTD) {
            IntegerTD other = (IntegerTD) obj;
            return intValue == other.getIntValue();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(getIntValue());
    }

}

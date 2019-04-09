/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.FloatTD;

/**
 * Implementation of {@link FloatTD}.
 * 
 * @author Doreen Seider
 */
public class FloatTDImpl extends AbstractTypedDatum implements FloatTD {

    private final double floatValue;

    public FloatTDImpl(double floatValue) {
        super(DataType.Float);
        this.floatValue = floatValue;
    }

    @Override
    public double getFloatValue() {
        return floatValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int randomDigit = 32;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(floatValue);
        result = prime * result + (int) (temp ^ (temp >>> randomDigit));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof FloatTD) {
            FloatTD other = (FloatTD) obj;
            return Double.doubleToLongBits(floatValue) == Double.doubleToLongBits(other.getFloatValue());
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(getFloatValue());
    }

}

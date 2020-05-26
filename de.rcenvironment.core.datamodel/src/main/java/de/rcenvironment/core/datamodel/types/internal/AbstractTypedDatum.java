/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Abstract base class for {@link TypedDatum} implementations.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractTypedDatum implements TypedDatum {

    private DataType dataType;

    public AbstractTypedDatum(DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

}

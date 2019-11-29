/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

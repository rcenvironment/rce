/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;

/**
 * Default {@link TypedDatumService} implementation.
 * 
 * @author Robert Mischke
 */
public class TypedDatumServiceImpl implements TypedDatumService {

    private TypedDatumConverter converter = new DefaultTypedDatumConverter();

    private TypedDatumSerializer serializer = new DefaultTypedDatumSerializer();

    private TypedDatumFactory factory = new DefaultTypedDatumFactory();

    @Override
    public TypedDatumConverter getConverter() {
        return converter;
    }

    @Override
    public TypedDatumFactory getFactory() {
        return factory;
    }

    @Override
    public TypedDatumSerializer getSerializer() {
        return serializer;
    }

}

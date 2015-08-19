/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;


/**
 * Service interface for {@link DataType} and {@link TypedDatum} operations.
 * 
 * @author Robert Mischke
 */
public interface TypedDatumService {

    /**
     * Returns the {@link TypedDatumConverter} all client code should use. May or may not return the
     * same instance on every call.
     * 
     * @return a ready-to-use {@link TypedDatumConverter} instance
     */
    TypedDatumConverter getConverter();

    /**
     * Returns the {@link TypedDatumFactory} all client code should use. May or may not return the
     * same instance on every call.
     * 
     * @return a ready-to-use {@link TypedDatumFactory} instance
     */
    TypedDatumFactory getFactory();

    /**
     * Returns the {@link TypedDatumSerializer} all client code should use. May or may not return
     * the same instance on every call.
     * 
     * @return a ready-to-use {@link TypedDatumSerializer} instance
     */
    TypedDatumSerializer getSerializer();
}

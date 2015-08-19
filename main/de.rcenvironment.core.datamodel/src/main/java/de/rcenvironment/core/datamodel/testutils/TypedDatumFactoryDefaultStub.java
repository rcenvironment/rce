/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.testutils;

import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.internal.DefaultTypedDatumFactory;

/**
 * Default mock for {@link TypedDatumFactory}. All methods behave like the instance returned by the
 * default {@link TypedDatumService#getFactory()} implementation.
 * 
 * @author Robert Mischke
 */
public class TypedDatumFactoryDefaultStub extends DefaultTypedDatumFactory {

}

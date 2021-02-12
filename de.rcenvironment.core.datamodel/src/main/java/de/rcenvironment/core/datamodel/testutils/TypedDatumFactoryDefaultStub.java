/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

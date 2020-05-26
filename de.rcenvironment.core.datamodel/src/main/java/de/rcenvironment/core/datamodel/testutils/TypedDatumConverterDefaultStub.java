/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.testutils;

import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.internal.DefaultTypedDatumConverter;

/**
 * Default mock for {@link TypedDatumConverter}. All methods behave like the instance returned by the
 * default {@link TypedDatumService#getConverter()} implementation.
 * 
 * @author Doreen Seider
 */
public class TypedDatumConverterDefaultStub extends DefaultTypedDatumConverter {

}

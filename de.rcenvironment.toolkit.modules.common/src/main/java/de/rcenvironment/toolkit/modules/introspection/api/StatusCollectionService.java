/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.introspection.api;

import de.rcenvironment.toolkit.utils.text.MultiLineOutput;

/**
 * A service providing combined status reports. This service is typically used to centrally log the application's overall state, or
 * print/log an unfinished operations overview on shutdown.
 * 
 * @author Robert Mischke
 */
public interface StatusCollectionService {

    /**
     * Creates a combined report of all available state information.
     * 
     * @return the multi-line report data
     */
    MultiLineOutput getCollectedDefaultStateInformation();

    /**
     * Creates a combined report of all available unfinished operations information.
     * 
     * @return the multi-line report data
     */
    MultiLineOutput getCollectedUnfinishedOperationsInformation();
}

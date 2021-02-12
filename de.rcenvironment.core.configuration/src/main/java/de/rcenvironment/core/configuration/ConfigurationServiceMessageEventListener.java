/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

/**
 * Listener interface to be implemented by components that want to get informed about errors
 * occurring in a {@link ConfigurationService}.
 * 
 * @author Christian Weiss
 */
public interface ConfigurationServiceMessageEventListener {

    /**
     * Handles a {@link ConfigurationServiceMessageEvent}.
     * 
     * @param error the error to handle
     */
    void handleConfigurationServiceError(ConfigurationServiceMessageEvent error);

}

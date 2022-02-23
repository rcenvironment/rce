/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration;

import java.io.File;

import de.rcenvironment.core.configuration.internal.ConfigurationStore;
import de.rcenvironment.core.configuration.internal.ConfigurationStoreImpl;

/**
 * 
 * Factory for creating {@link ConfigurationStore} instances.
 *
 * @author David Scholz
 */
public final class ConfigurationStoreFactory {
    
    private ConfigurationStoreFactory() {
        
    }
    
    /**
     * 
     * Factory method for {@link ConfigurationStore}.
     * 
     * @param storageFile config file.
     * @return implementation of {@link ConfigurationStore}
     */
    public static ConfigurationStore getConfigurationStore(File storageFile) {
        // there is no need for a distinction mechanism as there is only one implementation at the moment.
        return new ConfigurationStoreImpl(storageFile);
    }

}

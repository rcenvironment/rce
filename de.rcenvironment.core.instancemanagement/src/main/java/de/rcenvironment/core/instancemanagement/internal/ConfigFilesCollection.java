/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;

/**
 * Class to store all files necessary to configure an instance.
 * 
 * @author Lukas Rosenbach
 */
public class ConfigFilesCollection {
    
    private File configuration;
    private File components;
    
    public ConfigFilesCollection(File configuration, File components) {
        this.configuration = configuration;
        this.components = components;
    }
    
    public File getConfigurationFile() {
        return configuration;
    }
    
    public File getComponentsFile() {
        return components;
    }
}

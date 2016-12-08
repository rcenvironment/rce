/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.spi.module;

/**
 * Convenience base class for modules that do not have any configuration settings, and therefore use {@link Void} as their configuration
 * object type.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractZeroConfigurationToolkitModule extends AbstractToolkitModule<Void> {

    @Override
    public Void createConfigurationObject() {
        return null;
    }
}

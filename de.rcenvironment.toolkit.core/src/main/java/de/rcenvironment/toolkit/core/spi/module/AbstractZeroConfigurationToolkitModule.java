/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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

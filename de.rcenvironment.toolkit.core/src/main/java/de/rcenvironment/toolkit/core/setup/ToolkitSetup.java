/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.setup;

import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.spi.module.ToolkitModule;

/**
 * A minimal interface to define a {@link Toolkit}'s configuration before its creation.
 * 
 * @author Robert Mischke
 */
public interface ToolkitSetup {

    /**
     * Registers a {@link ToolkitModule}. The associated configuration object is automatically instantiated and returned (unless the type is
     * {@link Void}). This object can either be ignored to use the default settings, or modified up to the final {@link Toolkit} creation,
     * at which point the settings become final.
     * 
     * @param <T> the module's signature
     * @param <TModuleConf> the module's configuration object type
     * @param module the module class to register
     * @return the builder instance (for chaining)
     * @throws ToolkitException on internal errors
     */
    <T extends ToolkitModule<TModuleConf>, TModuleConf> TModuleConf configureModule(Class<T> module) throws ToolkitException;

}

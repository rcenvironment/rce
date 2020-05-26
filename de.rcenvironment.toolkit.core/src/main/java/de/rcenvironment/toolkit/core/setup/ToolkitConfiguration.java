/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.setup;

import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;

/**
 * The main interface for defining a {@link Toolkit}'s modules and their configuration. To add a module with its default configuration,
 * simply call setup.configureModule(&lt;module class>). If the module provides configuration options, a mutable configuration object is
 * returned from this call, which can then be used to apply the desired settings.
 * 
 * The design of this API is intended to support subclassing of configurations. A typical use case is having a "default" configuration, and
 * then subclassing it with more specific settings. For example, it may be useful to use a fixed-configuration {@link Toolkit} instance for
 * integration testing, but apply runtime configuration values for the {@link Toolkit} of the live application.
 * 
 * @author Robert Mischke
 */
public interface ToolkitConfiguration {

    /**
     * The main method to register modules and apply configuration settings (if applicable).
     * 
     * @param setup the setup instance to receive configuration calls
     * @throws ToolkitException on internal errors (for example, an error instantiating a module class)
     */
    void configure(ToolkitSetup setup) throws ToolkitException;
}

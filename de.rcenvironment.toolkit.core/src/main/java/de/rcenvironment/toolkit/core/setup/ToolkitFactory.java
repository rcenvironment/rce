/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.setup;

import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.internal.PicoContainerObjectGraph;
import de.rcenvironment.toolkit.core.internal.ToolkitBuilderImpl;
import de.rcenvironment.toolkit.core.internal.ToolkitInstanceTracker;

/**
 * Static entry point and factory for constructing {@link Toolkit} instances.
 * 
 * @author Robert Mischke
 */
public final class ToolkitFactory {

    private ToolkitFactory() {}

    /**
     * The single entry point to the factory that creates a {@link Toolkit} instance from a given {@link ToolkitConfiguration}. Typical
     * {@link ToolkitConfiguration} instances should be stateless, and can be used to create multiple independent {@link Toolkit} instances.
     * 
     * @param toolkitConfiguration the configuration to use for the new {@link Toolkit} instance
     * @return the new {@link Toolkit} instance
     * @throws ToolkitException on initialization errors, e.g. a configuration error, or lack of a suitable dependency injection library in
     *         the classpath
     */
    public static Toolkit create(ToolkitConfiguration toolkitConfiguration) throws ToolkitException {
        final ToolkitBuilderImpl builder = createBuilder();
        toolkitConfiguration.configure(builder);
        Toolkit newInstance = builder.create();
        ToolkitInstanceTracker.getInstance().register(newInstance);
        return newInstance;
    }

    private static ToolkitBuilderImpl createBuilder() throws ToolkitException {
        if (testForClass("org.picocontainer.PicoContainer")) {
            return new ToolkitBuilderImpl(new PicoContainerObjectGraph());
        } else {
            throw new ToolkitException("No supported dependency injection library (currently: PicoContainer 2.x) found in classpath");
        }
    }

    private static boolean testForClass(String className) {
        try {
            Class.forName(className, false, ToolkitFactory.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}

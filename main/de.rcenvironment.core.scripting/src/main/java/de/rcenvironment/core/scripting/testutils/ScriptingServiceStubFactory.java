/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting.testutils;

import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.internal.ScriptingServiceImpl;

/**
 * Provides {@link ScriptingService} instances for unit/integration tests.
 * 
 * @author Robert Mischke
 */
public final class ScriptingServiceStubFactory {

    private ScriptingServiceStubFactory() {}

    /**
     * Creates an instance of the actual {@link ScriptingService} implementation.
     * 
     * @return the new instance
     */
    public static ScriptingService createDefaultInstance() {
        return new ScriptingServiceImpl();
    }
}

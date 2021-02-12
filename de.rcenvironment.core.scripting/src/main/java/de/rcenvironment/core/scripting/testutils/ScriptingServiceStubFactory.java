/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting.testutils;

import javax.script.ScriptEngine;

import org.easymock.EasyMock;

import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.internal.ScriptingServiceImpl;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Provides {@link ScriptingService} instances for unit/integration tests.
 * 
 * @author Robert Mischke
 * @author Alexander Weinert
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

    /**
     * @param engine The engine to be returned from the mock
     * @return A mocked Scripting service that expects a single call to createScriptEngine and returns the given ScriptEngine
     */
    public static ScriptingService createDefaultMock(ScriptEngine engine) {
        final ScriptingService service = EasyMock.createStrictMock(ScriptingService.class);

        final ScriptLanguage expectedScriptLanguage = EasyMock.anyObject(ScriptLanguage.class);
        EasyMock.expect(service.createScriptEngine(expectedScriptLanguage)).andReturn(engine);

        EasyMock.replay(service);

        return service;
    }
}

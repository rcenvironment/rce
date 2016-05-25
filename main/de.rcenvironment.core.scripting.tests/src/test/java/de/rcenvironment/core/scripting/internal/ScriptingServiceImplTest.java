/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting.internal;

import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingServiceTest;

/**
 * Test for {@link ScriptingServiceImpl}.
 * 
 * @author Christian Weiss
 */
public class ScriptingServiceImplTest extends ScriptingServiceTest {

    @Override
    protected ScriptingService getService() {
        final ScriptingService result = new ScriptingServiceImpl();
        return result;
    }

}

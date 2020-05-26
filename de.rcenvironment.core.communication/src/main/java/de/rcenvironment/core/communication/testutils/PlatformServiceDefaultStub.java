/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;

/**
 * Default stub for {@link PlatformService}. All methods with a return value respond with the default field value for this type (null, 0,
 * false, '\u0000', ...).
 * 
 * This class (and subclasses of it) is intended for cases where an instance of {@link PlatformService} is required to set up the test, but
 * where the exact calls to this instance are not relevant. If they are relevant and should be tested, create a mock instance instead (for
 * example, with the EasyMock library).
 * 
 * @author Robert Mischke
 */
public class PlatformServiceDefaultStub implements PlatformService {

    @Override
    public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
        return null;
    }

    @Override
    public InstanceNodeId getLocalInstanceNodeId() {
        return null;
    }

    @Override
    public LogicalNodeId getLocalDefaultLogicalNodeId() {
        return null;
    }

    @Override
    public LogicalNodeSessionId getLocalDefaultLogicalNodeSessionId() {
        return null;
    }

    @Override
    public boolean matchesLocalInstance(ResolvableNodeId nodeId) {
        return false;
    }

}

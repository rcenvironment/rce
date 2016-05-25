/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Default stub for {@link PlatformService}. All methods with a return value respond with the
 * default field value for this type (null, 0, false, '\u0000', ...).
 * 
 * This class (and subclasses of it) is intended for cases where an instance of
 * {@link PlatformService} is required to set up the test, but where the exact calls to this
 * instance are not relevant. If they are relevant and should be tested, create a mock instance
 * instead (for example, with the EasyMock library).
 * 
 * @author Robert Mischke
 */
public class PlatformServiceDefaultStub implements PlatformService {

    @Override
    public NodeIdentifier getLocalNodeId() {
        return null;
    }

    // @Override
    // public NodeIdentityInformation getIdentityInformation() {
    // return null;
    // }

    @Override
    public boolean isLocalNode(NodeIdentifier nodeId) {
        return false;
    }
}

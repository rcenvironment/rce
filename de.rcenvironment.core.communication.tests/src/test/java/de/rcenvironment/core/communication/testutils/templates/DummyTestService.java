/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils.templates;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Pseudo-service interface for remote service testing.
 * 
 * @author Robert Mischke
 */
@RemotableService
public interface DummyTestService {

    /**
     * Dummy method call; no particular semantics.
     * 
     * @throws RemoteOperationException standard remote exception (required)
     */
    void dummyCall() throws RemoteOperationException;
}

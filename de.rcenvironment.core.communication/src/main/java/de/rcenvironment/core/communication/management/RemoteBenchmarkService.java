/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * A simple remote service that responds (via {@link #respond()}) to requests made as part of a benchmark run.
 * 
 * @author Robert Mischke
 */
@RemotableService
public interface RemoteBenchmarkService {

    /**
     * Generates the response to a benchmark request.
     * 
     * @param input the benchmark request payload; may or may not contain relevant data
     * @param respSize the expected response size to generate
     * @param respDelay the expected delay (in msec) before returning the response
     * @return the generated response payload
     * @throws RemoteOperationException standard {@link RemotableService} exception
     */
    Serializable respond(Serializable input, Integer respSize, Integer respDelay) throws RemoteOperationException;
}

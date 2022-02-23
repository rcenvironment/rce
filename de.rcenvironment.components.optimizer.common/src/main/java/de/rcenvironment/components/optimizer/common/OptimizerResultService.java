/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Service used to announce and receive values used for parameter study purposes.
 * @author Christian Weiss
 */
public interface OptimizerResultService {

    /**
     * Creates a {@link StudyPublisher}.
     * 
     * @param identifier the unique identifier
     * @param title the title
     * @param structure the structure definition of the values
     * @return the created {@link StudyPublisher}.
     */
    OptimizerPublisher createPublisher(final String identifier, final String title, final ResultStructure structure);

    /**
     * Create a {@link OptimizerReceiver}.
     * 
     * @param identifier the unique identifier
     * @param platform the platform to receive values from
     * @return the created {@link OptimizerReceiver}
     * @throws RemoteOperationException 
     */
    OptimizerReceiver createReceiver(final String identifier, final ResolvableNodeId platform) throws RemoteOperationException;

}

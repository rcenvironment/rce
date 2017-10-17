/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.common;

import de.rcenvironment.core.communication.common.ResolvableNodeId;

/**
 * Service used to announce and receive values used for parameter study purposes.
 * @author Christian Weiss
 */
public interface ParametricStudyService {

    /**
     * Creates a {@link StudyPublisher}.
     * 
     * @param identifier the unique identifier
     * @param title the title
     * @param structure the structure definition of the values
     * @return the created {@link StudyPublisher}.
     */
    StudyPublisher createPublisher(final String identifier, final String title, final StudyStructure structure);

    /**
     * Create a {@link StudyReceiver}.
     * 
     * @param identifier the unique identifier
     * @param platform the platform to receive values from
     * @return the created {@link StudyReceiver}
     */
    StudyReceiver createReceiver(final String identifier, final ResolvableNodeId platform);

}

/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;

import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Loads persistent workflow descriptions (aka ".wf files") into the in-memory {@link WorkflowDescription} representation, performing
 * workflow format updates if necessary.
 * <p>
 * Node: These methods were originally part of {@link WorkflowExecutionService}, but were then extracted into a separate service to improve
 * cohesion.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface PersistentWorkflowDescriptionLoaderService {

    /**
     * Loads {@link WorkflowDescription} from a {@link File}. It checks for updates and perform updates if needed.
     * 
     * @param wfFile the worflow file to load the {@link WorkflowDescription} from
     * @param callback a {@link WorkflowDescriptionLoaderCallback} to announce certain events during loading
     * @return {@link WorkflowDescription}
     * @throws WorkflowFileException if loading the {@link WorkflowDescription} from the file failed
     */
    WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException;

    /**
     * Loads {@link WorkflowDescription} from a {@link File}. It checks for updates and perform updates if needed.
     * 
     * @param wfFile the worflow file to load the {@link WorkflowDescription} from
     * @param callback a {@link WorkflowDescriptionLoaderCallback} to announce certain events during loading
     * @param abortIfWorkflowUpdateRequired whether a required workflow update should be considered an error
     * @return {@link WorkflowDescription}
     * @throws WorkflowFileException if loading the {@link WorkflowDescription} from the file failed
     */
    WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback,
        boolean abortIfWorkflowUpdateRequired) throws WorkflowFileException;

    /**
     * Loads {@link WorkflowDescription} from a {@link File}. It _doesn't_ check for updates and _doesn't_ perform updates at all.
     * 
     * @param wfFile the worflow file to load the {@link WorkflowDescription} from
     * @param callback a {@link WorkflowDescriptionLoaderCallback} to announce certain events during loading
     * @return {@link WorkflowDescription}
     * @throws WorkflowFileException if loading the {@link WorkflowDescription} from the file failed
     */
    WorkflowDescription loadWorkflowDescriptionFromFile(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException;
}

/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend;

import de.rcenvironment.core.datamanagement.RemotableMetaDataService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;

/**
 * Interface of the data management meta data backend.
 * 
 * @author Jan Flink
 * @author Brigitte Boden
 */
public interface MetaDataBackendService extends RemotableMetaDataService {

    /**
     * Key for a service property.
     */
    String PROVIDER = "de.rcenvironment.core.datamanagement.backend.metadata.provider";

    /**
     * Sets or updates the string representation of a timeline data item of an {@link WorkflowRun}.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}
     * @param timelineDataItem The string representation of the timeline data item.
     */
    void setOrUpdateTimelineDataItem(Long workflowRunId, String timelineDataItem);

    /**
     * Adds a {@link DataReference} to the {@link ComponentRun} with the given identifier.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param dataReference The {@link DataReference} to add.
     * @return The identifier of the generated dataReference
     */
    Long addDataReferenceToComponentRun(Long componentRunId, DataReference dataReference);

    /**
     * Adds a {@link DataReference} to the {@link ComponentInstance} with the given identifier.
     * 
     * @param componentInstanceId The identifier of the {@link ComponentInstance}.
     * @param dataReference The {@link DataReference} to add.
     * @return The identifier of the generated dataReference
     */
    Long addDataReferenceToComponentInstance(Long componentInstanceId, DataReference dataReference);

    /**
     * Adds a {@link DataReference} to the {@link ComponentRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link ComponentRun}.
     * @param dataReference The {@link DataReference} to add.
     * @return The identifier of the generated dataReference
     */
    Long addDataReferenceToWorkflowRun(Long workflowRunId, DataReference dataReference);

    /**
     * Gets the {@link DataReference} for the given uuid from the meta data backend.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @return The {@link DataReference}.
     */
    DataReference getDataReference(String dataReferenceKey);

    /**
     * Adds a {@link BinaryReference} to the {@link DataReference} with the given identifier.
     * 
     * @param dataReferenceId The identifier of the {@link DataReference}.
     * @param binaryReference The {@link BinaryReference} to add.
     */
    void addBinaryReference(Long dataReferenceId, BinaryReference binaryReference);
    
    /**
     * Checks if the meta data backend service was started successfully.
     * 
     * @return true, iff meta data backend service was started successfully.
     */
    boolean isMetaDataBackendOk();
    
    /**
     * Gets error message if the meta data backend service was not started successfully.
     * 
     * @return error message if the meta data backend service was not started successfully, and null, else.
     */
    String getMetaDataBackendStartErrorMessage();

}

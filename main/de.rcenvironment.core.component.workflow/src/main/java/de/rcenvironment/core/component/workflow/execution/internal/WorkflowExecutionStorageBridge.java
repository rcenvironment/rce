/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamanagement.commons.PropertiesKeys;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Bridge class to the data management, holding relevant data management ids.
 * 
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public class WorkflowExecutionStorageBridge {

    private static MetaDataBackendService metaDataBackendService;

    private static DataManagementService dataManagementService;

    private static TypedDatumService typedDatumService;

    private String errorMessageSuffix;

    private Long workflowDmId;

    private Map<String, Long> compInstDmIds;

    private Map<String, Map<String, Long>> inputDmIds = new HashMap<>();

    private Map<String, Map<String, Long>> outputDmIds = new HashMap<>();

    private Map<String, Long> intervalTypeDmIds = Collections.synchronizedMap(new HashMap<String, Long>());

    @Deprecated
    public WorkflowExecutionStorageBridge() {}

    public WorkflowExecutionStorageBridge(WorkflowExecutionContext wfExeCtx) {
        errorMessageSuffix = StringUtils.format(" of workflow '%s' (%s)", wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier());
    }

    protected Long addWorkflowExecution(WorkflowExecutionContext wfExeCtx, WorkflowDescription fullWorkflowDescription)
        throws WorkflowExecutionException {
        try {
            workflowDmId =
                metaDataBackendService.addWorkflowRun(wfExeCtx.getInstanceName(), wfExeCtx.getNodeId().getIdString(),
                    wfExeCtx.getDefaultStorageNodeId().getIdString(), System.currentTimeMillis());
            // Store additional information in the data management, if it is provided.
            if (wfExeCtx.getAdditionalInformationProvidedAtStart() != null) {
                Map<String, String> properties = new HashMap<>();
                properties.put(PropertiesKeys.ADDITIONAL_INFORMATION, wfExeCtx.getAdditionalInformationProvidedAtStart());
                metaDataBackendService.addWorkflowRunProperties(workflowDmId, properties);
            }
            try {
                MetaDataSet mds = new MetaDataSet();
                MetaData mdWorkflowRunId = new MetaData(MetaDataKeys.WORKFLOW_RUN_ID, true, true);
                mds.setValue(mdWorkflowRunId, workflowDmId.toString());
                WorkflowDescriptionPersistenceHandler persistenceHandler = new WorkflowDescriptionPersistenceHandler();
                ByteArrayOutputStream content = persistenceHandler.writeWorkflowDescriptionToStream(fullWorkflowDescription);
                String wfFileName = wfExeCtx.getWorkflowDescription().getFileName();
                if (wfFileName == null) {
                    // Set default name
                    wfFileName = "workflow.wf";
                }
                File wfFile =
                    TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(wfFileName);
                FileUtils.writeByteArrayToFile(wfFile, content.toByteArray());
                String wfFileReference =
                    dataManagementService.createReferenceFromLocalFile(wfFile, mds,
                        wfExeCtx.getNodeId());
                TypedDatum fileRefTD = typedDatumService.getFactory()
                    .createFileReference(wfFileReference, wfFile.getName());
                metaDataBackendService.addWorkflowFileToWorkflowRun(workflowDmId, typedDatumService.getSerializer().serialize(fileRefTD));
            } catch (IOException | InterruptedException | CommunicationException e) {
                throw new WorkflowExecutionException("Failed to store workflow file" + errorMessageSuffix, e);
            }
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (RemoteOperationException | RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store workflow execution" + errorMessageSuffix, e);
        }
        Map<String, Set<EndpointInstance>> compInputInstances = new HashMap<>();
        Map<String, Set<EndpointInstance>> compOutputInstances = new HashMap<>();
        Set<ComponentInstance> componentInstances = new HashSet<>();
        for (WorkflowNode wn : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
            Set<EndpointInstance> endpointInstances = new HashSet<>();
            String compExeId = wfExeCtx.getCompExeIdByWfNodeId(wn.getIdentifier());
            componentInstances.add(new ComponentInstance(compExeId, wn.getComponentDescription().getIdentifier(), wn.getName(), null));
            for (EndpointDescription ep : wn.getComponentDescription().getInputDescriptionsManager().getEndpointDescriptions()) {
                endpointInstances.add(new EndpointInstance(ep.getName(), EndpointType.INPUT));
            }
            compInputInstances.put(compExeId, endpointInstances);
            endpointInstances = new HashSet<>();
            for (EndpointDescription ep : wn.getComponentDescription().getOutputDescriptionsManager().getEndpointDescriptions()) {
                endpointInstances.add(new EndpointInstance(ep.getName(), EndpointType.OUTPUT));
            }
            compOutputInstances.put(compExeId, endpointInstances);
        }
        try {
            compInstDmIds = metaDataBackendService.addComponentInstances(workflowDmId, componentInstances);
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (RemoteOperationException | RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store component instances" + errorMessageSuffix, e);
        }
        for (String dmId : compInputInstances.keySet()) {
            try {
                inputDmIds.put(dmId, metaDataBackendService.addEndpointInstances(compInstDmIds.get(dmId),
                    compInputInstances.get(dmId)));
                // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
            } catch (RemoteOperationException | RuntimeException e) {
                throw new WorkflowExecutionException("Failed to store component input instances" + errorMessageSuffix, e);
            }
        }
        for (String compExeId : compOutputInstances.keySet()) {
            try {
                outputDmIds.put(compExeId, metaDataBackendService.addEndpointInstances(compInstDmIds.get(compExeId),
                    compOutputInstances.get(compExeId)));
                // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
            } catch (RemoteOperationException | RuntimeException e) {
                throw new WorkflowExecutionException("Failed to store component output instances" + errorMessageSuffix, e);
            }
        }

        // Store metadata for endpoints
        for (WorkflowNode wn : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
            String compExeId = wfExeCtx.getCompExeIdByWfNodeId(wn.getIdentifier());
            try {
                for (EndpointDescription ep : wn.getComponentDescription().getInputDescriptionsManager().getEndpointDescriptions()) {
                    metaDataBackendService.addEndpointInstanceProperties(inputDmIds.get(compExeId).get(ep.getName()),
                        ep.getMetaDataToPersist());
                }
                for (EndpointDescription ep : wn.getComponentDescription().getOutputDescriptionsManager().getEndpointDescriptions()) {
                    metaDataBackendService.addEndpointInstanceProperties(outputDmIds.get(compExeId).get(ep.getName()),
                        ep.getMetaDataToPersist());
                }
                // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
            } catch (RemoteOperationException | RuntimeException e) {
                throw new WorkflowExecutionException("Failed to store meta data for component output instances" + errorMessageSuffix, e);
            }
        }
        return workflowDmId;
    }

    protected void addWorkflowErrorLog(File logfile, String fileName) throws WorkflowExecutionException {
        try {
            MetaDataSet mds = new MetaDataSet();
            MetaData mdWorkflowRunId = new MetaData(MetaDataKeys.WORKFLOW_RUN_ID, true, true);
            mds.setValue(mdWorkflowRunId, workflowDmId.toString());

            String logFileReference = dataManagementService.createReferenceFromLocalFile(logfile, mds, null);
            FileReferenceTD fileReference = typedDatumService.getFactory().createFileReference(logFileReference, fileName);
            fileReference.setFileSize(FileUtils.sizeOf(logfile));
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(PropertiesKeys.ERROR_LOG_FILE, typedDatumService.getSerializer().serialize(fileReference));
            metaDataBackendService.addWorkflowRunProperties(workflowDmId, properties);
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (InterruptedException | IOException | CommunicationException | RemoteOperationException | RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store error log file." + errorMessageSuffix, e);
        }
    }

    protected void setWorkflowExecutionFinished(FinalWorkflowState finalState) throws WorkflowExecutionException {
        try {
            metaDataBackendService.setWorkflowRunFinished(workflowDmId, System.currentTimeMillis(), finalState);
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (RemoteOperationException | RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store final state" + errorMessageSuffix, e);
        }
        if (!intervalTypeDmIds.isEmpty()) {
            LogFactory.getLog(WorkflowExecutionStorageBridge.class).warn("Timeline interval ids left "
                + "which were not used for setting timeline interval to finished: " + intervalTypeDmIds);
        }
    }

    protected void addComponentTimelineInterval(TimelineIntervalType intervalType, long startTime, String compRunDmId)
        throws WorkflowExecutionException {
        synchronized (intervalTypeDmIds) {
            if (intervalTypeDmIds.containsKey(createTimelineIntervalMapKey(intervalType, compRunDmId))) {
                throw new WorkflowExecutionException("Timeline interval already written within this component run: " + intervalTypeDmIds);
            }
        }
        Long intervalTypeDmId;
        try {
            intervalTypeDmId = metaDataBackendService.addTimelineInterval(workflowDmId, intervalType, startTime, Long.valueOf(compRunDmId));
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (RemoteOperationException | RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store start of timeline interval" + errorMessageSuffix, e);
        }
        synchronized (intervalTypeDmIds) {
            intervalTypeDmIds.put(createTimelineIntervalMapKey(intervalType, compRunDmId), intervalTypeDmId);
        }
    }

    protected void setComponentTimelineIntervalFinished(TimelineIntervalType intervalType, long endTime, String compRunDmId)
        throws WorkflowExecutionException {
        synchronized (intervalTypeDmIds) {
            Long dmId = intervalTypeDmIds.remove(createTimelineIntervalMapKey(intervalType, compRunDmId));
            if (dmId != null) {
                try {
                    metaDataBackendService.setTimelineIntervalFinished(dmId, endTime);
                    // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
                } catch (RemoteOperationException | RuntimeException e) {
                    throw new WorkflowExecutionException("Failed to store end of timeline interval" + errorMessageSuffix, e);
                }
            } else {
                throw new WorkflowExecutionException(StringUtils.format("Failed to store end of timeline interval '%s' for component '%s'"
                    + " as no valid dm id exists", intervalType.name(), compRunDmId));
            }
        }
    }

    private String createTimelineIntervalMapKey(TimelineIntervalType intervalType, String compRunDmId) {
        return StringUtils.escapeAndConcat(compRunDmId, intervalType.name());
    }

    protected void addComponentCompleteLog(File completeLogfile, String fileName, String compRunDmId) throws ComponentExecutionException {
        addComponentLog(completeLogfile, fileName, compRunDmId, PropertiesKeys.COMPONENT_LOG_FILE);
    }

    protected void addComponentErrorLog(File errorLogfile, String fileName, String compRunDmId) throws ComponentExecutionException {
        addComponentLog(errorLogfile, fileName, compRunDmId, PropertiesKeys.COMPONENT_LOG_ERROR_FILE);
    }

    private void addComponentLog(File logfile, String fileName, String compRunDmId, String logFilePropertyKey)
        throws ComponentExecutionException {
        try {
            MetaDataSet mds = new MetaDataSet();
            MetaData compRunId = new MetaData(MetaDataKeys.COMPONENT_RUN_ID, true, true);
            mds.setValue(compRunId, compRunDmId);

            String logFileReference = dataManagementService.createReferenceFromLocalFile(logfile, mds, null);
            FileReferenceTD fileReference = typedDatumService.getFactory().createFileReference(logFileReference, fileName);
            fileReference.setFileSize(FileUtils.sizeOf(logfile));
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(logFilePropertyKey, typedDatumService.getSerializer().serialize(fileReference));
            metaDataBackendService.addComponentRunProperties(Long.valueOf(compRunDmId), properties);
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (InterruptedException | IOException | CommunicationException | RemoteOperationException | RuntimeException e) {
            throw new ComponentExecutionException("Failed to store component log file" + errorMessageSuffix, e);
        }
    }

    protected Long getWorkflowInstanceDataManamagementId() {
        return workflowDmId;
    }

    protected Long getComponentInstanceDataManamagementId(String compExecutionIdentifier) {
        return compInstDmIds.get(compExecutionIdentifier);
    }

    protected Map<String, Long> getInputInstanceDataManamagementIds(String compExecutionIdentifier) {
        return inputDmIds.get(compExecutionIdentifier);
    }

    protected Map<String, Long> getOutputInstanceDataManamagementIds(String compExecutionIdentifier) {
        return outputDmIds.get(compExecutionIdentifier);
    }

    protected void bindMetaDataService(MetaDataBackendService newService) {
        metaDataBackendService = newService;
    }

    protected void bindDataManagementService(DataManagementService newService) {
        dataManagementService = newService;
    }

    protected void bindTypedDatumService(TypedDatumService newService) {
        typedDatumService = newService;
    }

}

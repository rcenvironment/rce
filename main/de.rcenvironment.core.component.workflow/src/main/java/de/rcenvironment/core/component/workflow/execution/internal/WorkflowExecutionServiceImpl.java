/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateService;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateUtils;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Implementation of {@link WorkflowExecutionService}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private static final String FAILED_TO_LOAD_WORKFLOW_FILE = "Failed to load workflow file: ";

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionServiceImpl.class);

    /**
     * The interval (in msec) between the "heartbeat" notifications sent for active workflows.
     * Workflows are considered active when they are running or paused, or in the transitional
     * states in-between.
     */
    private static final int ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC = 6 * 1000;

    private static final int CACHE_SIZE = 20;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private CommunicationService communicationService;

    private DistributedNotificationService notificationService;

    private PersistentWorkflowDescriptionUpdateService wfUpdateService;

    private PlatformService platformService;

    private WorkflowHostService workflowHostService;

    private RemotableWorkflowExecutionControllerService wfExeCtrlService;

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private RemotableComponentExecutionControllerService componentExecutionControllerService;

    private Map<String, WeakReference<RemotableWorkflowExecutionControllerService>> wfExeCtrlServices = new LRUMap<>(CACHE_SIZE);

    private Set<WorkflowExecutionInformation> workflowExecutionInformations;

    private Object wfExeFetchLock = new Object();

    private ScheduledFuture<?> heartbeatSendFuture;

    protected void activate(BundleContext context) {

        heartbeatSendFuture = SharedThreadPool.getInstance().scheduleAtFixedRate(new Runnable() {

            @Override
            @TaskDescription("Send heartbeat for active workflows")
            public void run() {
                try {
                    Set<WorkflowExecutionInformation> wfExeInfoSnapshot = new HashSet<>(getExecutionControllerService(
                        platformService.getLocalNodeId()).getWorkflowExecutionInformations());
                    for (WorkflowExecutionInformation wfExeInfo : wfExeInfoSnapshot) {
                        String wfExeId = wfExeInfo.getExecutionIdentifier();
                        WorkflowState state = getExecutionControllerService(platformService.getLocalNodeId()).getWorkflowState(wfExeId);
                        switch (state) {
                        case INIT:
                        case PREPARING:
                        case PREPARED:
                        case STARTING:
                        case RUNNING:
                        case PAUSING:
                        case PAUSED:
                        case RESUMING:
                        case CANCELING:
                        case CANCELING_AFTER_FAILED:
                            if (verboseLogging) {
                                LOG.debug(StringUtils.format("Sending heartbeat notification for active workflow '%s' (%s)",
                                    wfExeInfo.getInstanceName(), wfExeId));
                            }
                            notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeId, WorkflowState.IS_ALIVE.name());
                            break;
                        default:
                            // do nothing
                            break;
                        }
                    }
                } catch (ExecutionControllerException | RemoteOperationException e) {
                    throw new IllegalStateException("Failed to get local workflow execution information or workflow states: "
                        + e.toString());
                }
            }
        }, ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC);
    }

    protected void deactivate() {
        if (heartbeatSendFuture != null) {
            heartbeatSendFuture.cancel(true);
        }
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException {

        try {
            int wfVersion = readWorkflowVersionNumber(wfFile);
            if (wfVersion > WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER) {
                throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath()
                    + StringUtils.format(". Its version (%d) is newer than the expected"
                        + " one (%d). Most likely reason: it was opened with a newer version of RCE before.",
                        wfVersion, WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER));
                    
            }
            
            try (InputStream fileInputStream = new FileInputStream(wfFile)) {
                
                PersistentWorkflowDescription persistentDescription = wfUpdateService.createPersistentWorkflowDescription(
                    IOUtils.toString(fileInputStream, WorkflowConstants.ENCODING_UTF8));

                boolean updateRequired = wfUpdateService.isUpdateForWorkflowDescriptionAvailable(persistentDescription, false);
                boolean nonSilentUpdateRequired = updateRequired;

                if (!nonSilentUpdateRequired) {
                    updateRequired = wfUpdateService.isUpdateForWorkflowDescriptionAvailable(persistentDescription, true);
                }
                if (updateRequired) {
                    String backupFilename = null;
                    if (nonSilentUpdateRequired) {
                        backupFilename = PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(wfFile) + ".wf";
                        FileUtils.copyFile(wfFile, new File(wfFile.getParentFile().getAbsolutePath(), backupFilename));
                    }
                    try {
                        updateWorkflow(persistentDescription, wfFile, nonSilentUpdateRequired);
                        onWorkflowFileUpdated(wfFile, !nonSilentUpdateRequired, backupFilename, callback);
                    } catch (IOException | RuntimeException e) {
                        if (nonSilentUpdateRequired) {
                            throw new WorkflowFileException(StringUtils.format("Failed to update workflow file: %s. Backup file "
                                + "was generated: %s.", wfFile.getAbsolutePath(), backupFilename), e);
                        } else {
                            throw new WorkflowFileException(StringUtils.format("Failed to update workflow file: %s.",
                                wfFile.getAbsolutePath()), e);
                        }
                    }
                }
            }
            return loadWorkflowDescriptionFromFile(wfFile, callback);
        } catch (IOException | ParseException e) {
            throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath(), e);
        }
    }
    
    private int readWorkflowVersionNumber(File wfFile) throws ParseException, IOException {
        try (InputStream fileInputStream = new FileInputStream(wfFile)) {
            return new WorkflowDescriptionPersistenceHandler().readWorkflowVersionNumber(fileInputStream);
        }
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFile(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException {
        try {
            int wfVersion = readWorkflowVersionNumber(wfFile);
            if (wfVersion > WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER) {
                throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath()
                    + StringUtils.format(". Its version (%d) is older than the expected"
                        + " one (%d). Most likely reason: Internal error on workflow update.",
                        wfVersion, WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER));
            }
            WorkflowDescription wd;
            try (InputStream fileInputStream = new FileInputStream(wfFile)) {
                wd = new WorkflowDescriptionPersistenceHandler().readWorkflowDescriptionFromStream(fileInputStream);
            } catch (WorkflowFileException e) {
                if (e.getParsedWorkflowDescription() != null && callback.arePartlyParsedWorkflowConsiderValid()) {
                    String backupFilename = PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(wfFile) + ".wf";
                    FileUtils.copyFile(wfFile, new File(wfFile.getParentFile().getAbsolutePath(), backupFilename));
                    callback.onWorkflowFileParsingPartlyFailed(backupFilename);
                    wd = e.getParsedWorkflowDescription();
                } else {
                    throw e;
                }
            }
            return wd;
        } catch (IOException | ParseException e) {
            throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath(), e);
        }
    }

    private void onWorkflowFileUpdated(File wfFile, boolean silentUpdate, String backupFilename,
        WorkflowDescriptionLoaderCallback callback) {
        if (silentUpdate) {
            String message = StringUtils.format("Workflow file is updated (silent update): %s", wfFile.getAbsolutePath());
            LOG.debug(message);
            callback.onSilentWorkflowFileUpdated(message);
        } else {
            String message = StringUtils.format("Workflow file is updated (non-silent update):"
                + " %s. Backup file is generated: %s", wfFile.getAbsolutePath(), backupFilename);
            LOG.debug(message);
            callback.onNonSilentWorkflowFileUpdated(message, backupFilename);
        }
    }

    private void updateWorkflow(PersistentWorkflowDescription persWfDescr, File file, boolean hasNonSilentUpdate) throws IOException {
        try (InputStream tempInputStream = IOUtils.toInputStream(wfUpdateService
            .performWorkflowDescriptionUpdate(persWfDescr).getWorkflowDescriptionAsString(), WorkflowConstants.ENCODING_UTF8)) {
            FileUtils.write(file, IOUtils.toString(tempInputStream));
            tempInputStream.close();
        }
    }

    @Override
    public WorkflowDescriptionValidationResult validateWorkflowDescription(WorkflowDescription workflowDescription) {
        NodeIdentifier missingControllerNodeId = null;
        Map<String, NodeIdentifier> missingComponentsNodeIds = null;

        NodeIdentifier controllerNode = workflowDescription.getControllerNode();
        if (controllerNode == null) {
            controllerNode = platformService.getLocalNodeId();
        }
        if (!workflowHostService.getWorkflowHostNodesAndSelf().contains(controllerNode)) {
            missingControllerNodeId = controllerNode;
        }

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();

        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            NodeIdentifier componentNode = node.getComponentDescription().getNode();
            if (componentNode == null) {
                componentNode = platformService.getLocalNodeId();
            }
            if (!ComponentUtils.hasComponent(compKnowledge.getAllInstallations(), node.getComponentDescription().getIdentifier(),
                componentNode)) {
                if (missingComponentsNodeIds == null) {
                    missingComponentsNodeIds = new HashMap<>();
                }
                missingComponentsNodeIds.put(node.getName(), componentNode);
            }
        }
        if (missingControllerNodeId == null && missingComponentsNodeIds == null) {
            return WorkflowDescriptionValidationResult.createResultForSuccess();
        } else {
            return WorkflowDescriptionValidationResult.createResultForFailure(missingControllerNodeId, missingComponentsNodeIds);
        }
    }

    @Override
    public WorkflowExecutionInformation executeWorkflowAsync(WorkflowExecutionContext wfExeCtx)
        throws WorkflowExecutionException, RemoteOperationException {
        WorkflowExecutionInformation workflowExecutionInformation = createExecutionController(wfExeCtx);
        try {
            performStartOnExecutionController(workflowExecutionInformation.getExecutionIdentifier(), wfExeCtx.getNodeId());
        } catch (ExecutionControllerException e) {
            throw new WorkflowExecutionException("Failed to execute workflow", e);
        }
        return workflowExecutionInformation;
    }

    private WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext wfExeCtx) throws RemoteOperationException,
        WorkflowExecutionException {

        Map<String, String> authTokens = createAndRegisterLocalComponentExecutionAuthTokens((wfExeCtx).getWorkflowDescription());
        return getExecutionControllerService(wfExeCtx.getNodeId()).createExecutionController(wfExeCtx, authTokens,
            !platformService.isLocalNode(wfExeCtx.getNodeId()));
    }

    /**
     * Creates an auth token for each component which must be instantiated locally from an remote
     * workflow execution controller that was instantiated from local node. It ensures that local
     * components, which were not published, can be instantiated from remote, but only from workflow
     * execution controllers, which were created from local node and thus, which are allowed to
     * instantiate local components even if they are not published.
     */
    private Map<String, String> createAndRegisterLocalComponentExecutionAuthTokens(WorkflowDescription workflowDescription) {
        Map<String, String> compIdToTokenMapping = new HashMap<String, String>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            NodeIdentifier node = wfNode.getComponentDescription().getNode();
            // Use empty string instead of null to avoid "remote method not found" issue. If null is
            // passed the method can not be inspected
            String token = "";
            if (node == null || platformService.isLocalNode(node)) {
                token = UUID.randomUUID().toString();
                try {
                    componentExecutionControllerService.addComponentExecutionAuthToken(token);
                } catch (RemoteOperationException e) {
                    // should not happen as it is finally local call
                    throw new IllegalStateException("Failed to add auth tokens for component execution; cause: " + e.toString());
                }
            }
            compIdToTokenMapping.put(wfNode.getIdentifier(), token);
        }
        return compIdToTokenMapping;
    }

    private void performStartOnExecutionController(String executionId, NodeIdentifier node) throws ExecutionControllerException,
        RemoteOperationException {
        getExecutionControllerService(node).performStart(executionId);
    }

    @Override
    public void cancel(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performCancel(executionId);
    }

    @Override
    public void pause(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPause(executionId);
    }

    @Override
    public void resume(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performResume(executionId);
    }

    @Override
    public void dispose(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performDispose(executionId);
    }

    @Override
    public WorkflowState getWorkflowState(String executionId, NodeIdentifier node) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(node).getWorkflowState(executionId);
    }

    @Override
    public Long getWorkflowDataManagementId(String executionId, NodeIdentifier node) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(node).getWorkflowDataManagementId(executionId);
    }

    @Override
    public Set<WorkflowExecutionInformation> getLocalWorkflowExecutionInformations() {
        try {
            return new HashSet<WorkflowExecutionInformation>(wfExeCtrlService.getWorkflowExecutionInformations());
        } catch (ExecutionControllerException | RemoteOperationException e) {
            // should not happen as it is finally a local call and the ExecutionController are directly fetched before
            throw new IllegalStateException("Failed to get local workflow execution information; cause: " + e.toString());
        }
    }

    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations() {
        return getWorkflowExecutionInformations(false);
    }

    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations(boolean forceRefresh) {
        if (!forceRefresh && workflowExecutionInformations != null) {
            return new HashSet<>(workflowExecutionInformations);
        } else {
            synchronized (wfExeFetchLock) {
                if (forceRefresh || workflowExecutionInformations == null) {
                    Set<WorkflowExecutionInformation> tempWfExeInfos = new HashSet<>();

                    CallablesGroup<Collection> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Collection.class);

                    for (NodeIdentifier node : workflowHostService.getWorkflowHostNodesAndSelf()) {
                        final NodeIdentifier finalNode = node;
                        callablesGroup.add(new Callable<Collection>() {

                            @Override
                            @TaskDescription("Distributed query: getWorkflowInformations()")
                            public Collection call() throws Exception {
                                RemotableWorkflowExecutionControllerService executionControllerService =
                                    getExecutionControllerService(finalNode);
                                try {
                                    return executionControllerService.getWorkflowExecutionInformations();
                                } catch (RemoteOperationException e) {
                                    LOG.error(StringUtils.format("Failed to query remote workflows on node %s; cause: %s",
                                        finalNode, e.toString()));
                                }
                                return null;
                            }
                        });
                    }
                    List<Collection> results = callablesGroup.executeParallel(new AsyncExceptionListener() {

                        @Override
                        public void onAsyncException(Exception e) {
                            LOG.warn("Exception during asynchrous execution", e);
                        }
                    });
                    // merge results
                    for (Collection singleResult : results) {
                        if (singleResult != null) {
                            tempWfExeInfos.addAll(singleResult);
                        }
                    }
                    workflowExecutionInformations = tempWfExeInfos;
                }
                return new HashSet<>(workflowExecutionInformations);
            }
        }

    }

    private RemotableWorkflowExecutionControllerService getExecutionControllerService(NodeIdentifier node) throws RemoteOperationException {
        RemotableWorkflowExecutionControllerService wfCtrlService = null;
        synchronized (wfExeCtrlServices) {
            if (wfExeCtrlServices.containsKey(node.getIdString())) {
                wfCtrlService = wfExeCtrlServices.get(node.getIdString()).get();
            }
            if (wfCtrlService == null) {
                wfCtrlService = communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, node);
                wfExeCtrlServices.put(node.getIdString(), new WeakReference<RemotableWorkflowExecutionControllerService>(wfCtrlService));
            }
        }
        return wfCtrlService;
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    protected void bindNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }

    protected void bindPersistentWorkflowDescriptionUpdateService(PersistentWorkflowDescriptionUpdateService newService) {
        wfUpdateService = newService;
    }

    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    protected void bindComponentExecutionControllerService(RemotableComponentExecutionControllerService newService) {
        componentExecutionControllerService = newService;
    }

    protected void bindWorkflowHostService(WorkflowHostService newService) {
        workflowHostService = newService;
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        componentKnowledgeService = newService;
    }

    protected void bindWorkflowExecutionControllerService(RemotableWorkflowExecutionControllerService newService) {
        wfExeCtrlService = newService;
    }

}

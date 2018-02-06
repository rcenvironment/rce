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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
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
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link WorkflowExecutionService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private static final String FAILED_TO_LOAD_WORKFLOW_FILE = "Failed to load workflow file: ";

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionServiceImpl.class);

    /**
     * The interval (in msec) between the "heartbeat" notifications sent for active workflows. Workflows are considered active when they are
     * running or paused, or in the transitional states in-between.
     */
    private static final int ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC = 6 * 1000;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private CommunicationService communicationService;

    private DistributedNotificationService notificationService;

    private PersistentWorkflowDescriptionUpdateService wfUpdateService;

    private PlatformService platformService;

    private WorkflowHostService workflowHostService;

    private RemotableWorkflowExecutionControllerService wfExeCtrlService;

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private RemotableComponentExecutionControllerService componentExecutionControllerService;

    private Set<WorkflowExecutionInformation> workflowExecutionInformations;

    private Object wfExeFetchLock = new Object();

    private ScheduledFuture<?> heartbeatSendFuture;

    protected void activate(BundleContext context) {

        heartbeatSendFuture = ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedRate(new Runnable() {

            @Override
            @TaskDescription("Send heartbeat for active workflows")
            public void run() {
                Set<WorkflowExecutionInformation> wfExeInfoSnapshot = getWorkflowExecutionInformation();
                for (WorkflowExecutionInformation wfExeInfo : wfExeInfoSnapshot) {
                    String wfExeId = wfExeInfo.getExecutionIdentifier();
                    switch (wfExeInfo.getWorkflowState()) {
                    case INIT:
                    case PREPARING:
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
            }

            private Set<WorkflowExecutionInformation> getWorkflowExecutionInformation() {
                Set<WorkflowExecutionInformation> wfExeInfoSnapshot = new HashSet<>();
                try {
                    wfExeInfoSnapshot.addAll(wfExeCtrlService.getWorkflowExecutionInformations());
                } catch (ExecutionControllerException | RemoteOperationException e) {
                    LOG.error("Failed to fetch local workflow execution informations: " + e.getMessage());
                }
                return wfExeInfoSnapshot;
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
        // delegate
        return loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, callback, false);
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback,
        boolean abortIfWorkflowUpdateRequired) throws WorkflowFileException {

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

                if (updateRequired && abortIfWorkflowUpdateRequired) {
                    throw new WorkflowFileException(
                        "The workflow file "
                            + wfFile.getAbsolutePath()
                            + " would require an update before execution, but the 'fail on required update' flag has been set. "
                            + "Typically, this means that it was generated from an internal template which should be updated.");
                }

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
            WorkflowDescriptionPersistenceHandler wdPesistenceHandler = new WorkflowDescriptionPersistenceHandler();
            WorkflowDescription wd;
            try (InputStream fileInputStream = new FileInputStream(wfFile)) {
                wd = wdPesistenceHandler.readWorkflowDescriptionFromStream(fileInputStream);
            } catch (WorkflowFileException e) {
                if (e.getParsedWorkflowDescription() != null && callback.arePartlyParsedWorkflowConsiderValid()) {
                    // backup the orginal workflow file and overwrite it with the reduced but valid workflow description
                    String backupFilename =
                        PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(wfFile) + WorkflowConstants.WORKFLOW_FILE_ENDING;
                    FileUtils.copyFile(wfFile, new File(wfFile.getParentFile().getAbsolutePath(), backupFilename));
                    wd = e.getParsedWorkflowDescription();
                    try (FileOutputStream fos = new FileOutputStream(wfFile);
                        ByteArrayOutputStream baos = wdPesistenceHandler.writeWorkflowDescriptionToStream(wd)) {
                        baos.writeTo(fos);
                    }
                    callback.onWorkflowFileParsingPartlyFailed(backupFilename);
                } else {
                    throw e;
                }
            }
            return wd;
        } catch (IOException | ParseException | RuntimeException e) {
            throw new WorkflowFileException(FAILED_TO_LOAD_WORKFLOW_FILE + wfFile.getAbsolutePath(), e);
        }
    }

    private void onWorkflowFileUpdated(File wfFile, boolean silentUpdate, String backupFilename,
        WorkflowDescriptionLoaderCallback callback) {
        if (silentUpdate) {
            String message = StringUtils.format("'%s' was updated (silently) (full path: %s)", wfFile.getName(), wfFile.getAbsolutePath());
            LOG.debug(message);
            callback.onSilentWorkflowFileUpdated(message);
        } else {
            String message =
                StringUtils.format("'%s' was updated (non-silently); backup file generated: %s (full path: %s)", wfFile.getName(),
                    backupFilename, wfFile.getAbsolutePath());
            LOG.debug(message);
            callback.onNonSilentWorkflowFileUpdated(message, backupFilename);
        }
    }

    /**
     * Invokes the update of the workflow description and stores the updated workflow description in the specified file.
     */
    private void updateWorkflow(PersistentWorkflowDescription persWfDescr, File file, boolean hasNonSilentUpdate) throws IOException {
        try (InputStream tempInputStream = IOUtils.toInputStream(wfUpdateService
            .performWorkflowDescriptionUpdate(persWfDescr).getWorkflowDescriptionAsString(), WorkflowConstants.ENCODING_UTF8)) {
            FileUtils.write(file, IOUtils.toString(tempInputStream));
            tempInputStream.close();
        }
    }

    @Override
    public WorkflowDescriptionValidationResult validateWorkflowDescription(WorkflowDescription workflowDescription) {
        LogicalNodeId missingControllerNodeId = null;
        Map<String, LogicalNodeId> missingComponentsNodeIds = new HashMap<>();

        LogicalNodeId controllerNode = workflowDescription.getControllerNode();
        if (controllerNode == null) {
            controllerNode = platformService.getLocalDefaultLogicalNodeId();
        }
        if (!workflowHostService.getLogicalWorkflowHostNodesAndSelf().contains(controllerNode)) {
            missingControllerNodeId = controllerNode;
        }

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();

        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            LogicalNodeId componentNode = node.getComponentDescription().getNode();
            if (componentNode == null) {
                componentNode = platformService.getLocalDefaultLogicalNodeId();
            }
            if (!ComponentUtils.hasComponent(compKnowledge.getAllInstallations(), node.getComponentDescription().getIdentifier(),
                componentNode)) {
                missingComponentsNodeIds.put(node.getName(), componentNode);
            }
        }
        if (missingControllerNodeId == null && missingComponentsNodeIds.isEmpty()) {
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
            !platformService.matchesLocalInstance(wfExeCtx.getNodeId()));
    }

    /**
     * Creates an auth token for each component which must be instantiated locally from an remote workflow execution controller that was
     * instantiated from local node. It ensures that local components, which were not published, can be instantiated from remote, but only
     * from workflow execution controllers, which were created from local node and thus, which are allowed to instantiate local components
     * even if they are not published.
     */
    private Map<String, String> createAndRegisterLocalComponentExecutionAuthTokens(WorkflowDescription workflowDescription) {
        Map<String, String> compIdToTokenMapping = new HashMap<String, String>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            LogicalNodeId node = wfNode.getComponentDescription().getNode();
            // Use empty string instead of null to avoid "remote method not found" issue. If null is
            // passed the method can not be inspected
            String token = "";
            if (node == null || platformService.matchesLocalInstance(node)) {
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

    private void performStartOnExecutionController(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
        RemoteOperationException {
        getExecutionControllerService(node).performStart(executionId);
    }

    @Override
    public void cancel(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performCancel(executionId);
    }

    @Override
    public void pause(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPause(executionId);
    }

    @Override
    public void resume(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performResume(executionId);
    }

    @Override
    public void dispose(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performDispose(executionId);
    }

    @Override
    public WorkflowState getWorkflowState(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(node).getWorkflowState(executionId);
    }

    @Override
    public Long getWorkflowDataManagementId(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
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

                    CallablesGroup<Collection> callablesGroup =
                        ConcurrencyUtils.getFactory().createCallablesGroup(Collection.class);

                    for (InstanceNodeSessionId node : workflowHostService.getWorkflowHostNodesAndSelf()) {
                        final InstanceNodeSessionId finalNode = node;
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

    private RemotableWorkflowExecutionControllerService getExecutionControllerService(ResolvableNodeId node)
        throws RemoteOperationException {
        // fetching the service proxy on each call, assuming that it will be cached centrally if necessary
        return communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, node);
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

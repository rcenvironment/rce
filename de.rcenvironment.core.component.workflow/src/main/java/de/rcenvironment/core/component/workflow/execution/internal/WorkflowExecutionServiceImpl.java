/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.authorization.api.ComponentExecutionAuthorizationService;
import de.rcenvironment.core.component.authorization.api.RemotableComponentExecutionAuthorizationService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
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
@Component
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    // TODO make non-static
    private static final Log LOG = LogFactory.getLog(WorkflowExecutionServiceImpl.class);

    /**
     * The interval (in msec) between the "heartbeat" notifications sent for active workflows. Workflows are considered active when they are
     * running or paused, or in the transitional states in-between.
     */
    private static final int ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC = 6 * 1000;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled("WorkflowExecution");

    private CommunicationService communicationService;

    private DistributedNotificationService notificationService;

    private PersistentWorkflowDescriptionLoaderService workflowDescriptionLoaderService;

    private PlatformService platformService;

    private WorkflowHostService workflowHostService;

    private RemotableWorkflowExecutionControllerService wfExeCtrlService;

    private ComponentExecutionAuthorizationService componentExecutionAuthorizationService;

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private AuthorizationService authorizationService;

    private CryptographyOperationsProvider cryptographyOperationsProvider;

    private RemotableComponentExecutionControllerService componentExecutionControllerService;

    private Set<WorkflowExecutionInformation> workflowExecutionInformations;

    private Object wfExeFetchLock = new Object();

    private ScheduledFuture<?> heartbeatSendFuture;

    private MetaDataService metaDataService;

    @Activate
    protected void activate(BundleContext context) {

        heartbeatSendFuture =
            ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedInterval("Send heartbeat for active workflows",
                this::sendHeartbeatForActiveWorkflows, ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC);
    }

    @Deactivate
    protected void deactivate() {
        if (heartbeatSendFuture != null) {
            heartbeatSendFuture.cancel(true);
        }
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException {
        // delegate for backwards compatibility
        return workflowDescriptionLoaderService.loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, callback);
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback,
        boolean abortIfWorkflowUpdateRequired) throws WorkflowFileException {
        // delegate for backwards compatibility
        return workflowDescriptionLoaderService.loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, callback,
            abortIfWorkflowUpdateRequired);
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFile(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException {
        // delegate for backwards compatibility
        return workflowDescriptionLoaderService.loadWorkflowDescriptionFromFile(wfFile, callback);
    }

    @Override
    public WorkflowDescriptionValidationResult validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(
        WorkflowDescription workflowDescription) {
        LogicalNodeId missingControllerNodeId = null;
        Map<String, LogicalNodeId> missingComponentsNodeIds = new HashMap<>();

        LogicalNodeId controllerNode = workflowDescription.getControllerNode();
        if (controllerNode == null) {
            controllerNode = platformService.getLocalDefaultLogicalNodeId();
        }
        if (!workflowHostService.getLogicalWorkflowHostNodesAndSelf().contains(controllerNode)) {
            missingControllerNodeId = controllerNode;
        }

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();

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
    public Map<String, String> validateRemoteWorkflowControllerVisibilityOfComponents(WorkflowDescription wfDescription) {
        List<String> componentRefs = new ArrayList<>();
        for (WorkflowNode wfDescNode : wfDescription.getWorkflowNodes()) {
            LogicalNodeId compLocation = wfDescNode.getComponentDescription().getNode();
            if (platformService.matchesLocalInstance(compLocation)) {
                // components on the initiating instance do not need to be checked as they grant the controller special access -- misc_ro
                continue;
            }
            componentRefs.add(StringUtils.escapeAndConcat(
                // 0: component id (for display grouping)
                wfDescNode.getIdentifierAsObject().toString(),
                // 1: component id and version
                wfDescNode.getComponentIdentifierWithVersion(),
                // 2: location (node id)
                compLocation.getLogicalNodeIdString()));
        }
        RemotableWorkflowExecutionControllerService remoteWFExecControllerService =
            communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, wfDescription.getControllerNode());
        try {
            return remoteWFExecControllerService.verifyComponentVisibility(componentRefs);
        } catch (RemoteOperationException e) {
            Map<String, String> result = new HashMap<>();
            for (WorkflowNode wfDescNode : wfDescription.getWorkflowNodes()) {
                result.put(wfDescNode.getIdentifierAsObject().toString(),
                    "Failed to query the selected workflow controller about component visibility: " + e.getMessage());
            }
            return result;
        }
    }

    @Override
    // Implementation note: this is currently the first method that is shared between the headless and GUI execution paths
    public WorkflowExecutionInformation startWorkflowExecution(WorkflowExecutionContext wfExeCtx)
        throws WorkflowExecutionException, RemoteOperationException {
        WorkflowExecutionInformation workflowExecutionInformation = createExecutionController(wfExeCtx);
        try {
            performStartOnExecutionController(workflowExecutionInformation.getWorkflowExecutionHandle());
        } catch (ExecutionControllerException e) {
            throw new WorkflowExecutionException("Failed to execute workflow", e);
        }
        return workflowExecutionInformation;
    }

    private WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext wfExeCtx) throws RemoteOperationException,
        WorkflowExecutionException {

        Map<String, String> authTokens = acquireExecutionAuthorizationTokensForComponents(wfExeCtx.getWorkflowDescription());

        return getExecutionControllerService(wfExeCtx.getNodeId()).createExecutionController(wfExeCtx, authTokens,
            !platformService.matchesLocalInstance(wfExeCtx.getNodeId()));
    }

    private void performStartOnExecutionController(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performStart(handle.getIdentifier());
    }

    @Override
    public void cancel(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performCancel(handle.getIdentifier());
    }

    @Override
    public void pause(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performPause(handle.getIdentifier());
    }

    @Override
    public void resume(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performResume(handle.getIdentifier());
    }

    @Override
    public void dispose(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performDispose(handle.getIdentifier());
    }

    @Override
    public void deleteFromDataManagement(WorkflowExecutionHandle handle) throws ExecutionControllerException {
        Long wfDataManagementId;
        try {
            wfDataManagementId = getWorkflowDataManagementId(handle);
        } catch (ExecutionControllerException | RemoteOperationException e) {
            throw new ExecutionControllerException("Failed to determine the storage id of workflow run " + handle.getIdentifier(), e);
        }
        try {
            // note: this relies on the current convention that the storage location is always the wf controller's location
            metaDataService.deleteWorkflowRun(wfDataManagementId, handle.getLocation());
        } catch (CommunicationException e) {
            throw new ExecutionControllerException("Could not delete workflow run " + wfDataManagementId, e);
        }
    }

    @Override
    public WorkflowState getWorkflowState(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(handle.getLocation()).getWorkflowState(handle.getIdentifier());
    }

    @Override
    public Long getWorkflowDataManagementId(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(handle.getLocation()).getWorkflowDataManagementId(handle.getIdentifier());
    }

    @Override
    public Set<WorkflowExecutionInformation> getLocalWorkflowExecutionInformations() {
        try {
            return new HashSet<>(wfExeCtrlService.getWorkflowExecutionInformations());
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

    @Reference
    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    @Reference
    protected void bindNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }

    @Reference
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    @Reference
    protected void bindComponentExecutionControllerService(RemotableComponentExecutionControllerService newService) {
        componentExecutionControllerService = newService;
    }

    @Reference
    protected void bindPersistentWorkflowDescriptionLoaderService(PersistentWorkflowDescriptionLoaderService newService) {
        workflowDescriptionLoaderService = newService;
    }

    @Reference
    protected void bindWorkflowHostService(WorkflowHostService newService) {
        workflowHostService = newService;
    }

    @Reference
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        componentKnowledgeService = newService;
    }

    @Reference
    protected void bindWorkflowExecutionControllerService(RemotableWorkflowExecutionControllerService newService) {
        wfExeCtrlService = newService;
    }

    @Reference
    protected void bindMetaDataService(MetaDataService newService) {
        metaDataService = newService;
    }

    @Reference
    protected void bindComponentExecutionAuthorizationService(ComponentExecutionAuthorizationService newService) {
        this.componentExecutionAuthorizationService = newService;
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newService) {
        this.authorizationService = newService;
    }

    @Reference
    protected void bindCryptographyOperationsProvider(CryptographyOperationsProvider newService) {
        this.cryptographyOperationsProvider = newService;
    }

    private Map<String, String> acquireExecutionAuthorizationTokensForComponents(WorkflowDescription workflowDescription)
        throws WorkflowExecutionException {

        final Map<String, String> resultMap = new HashMap<>(); // access must be synchronized by caller
        DistributedComponentKnowledge distrCompKnowledge = componentKnowledgeService.getCurrentSnapshot();

        final CallablesGroup<WorkflowExecutionException> callablesGroup =
            ConcurrencyUtils.getFactory().createCallablesGroup(WorkflowExecutionException.class);
        for (WorkflowNode wfDescriptionNode : workflowDescription.getWorkflowNodes()) {

            // do not acquire authorization tokens for disabled components
            // note that when redesigning the workflow engine, this sort of exclusion should happen centrally -- misc_ro
            if (!wfDescriptionNode.isEnabled()) {
                continue;
            }

            // TODO convert to lambda after API migration is complete
            // TODO this would really benefit from an "execute with standard exception handling" API
            callablesGroup.add(new Callable<WorkflowExecutionException>() {

                @Override
                @TaskDescription("Acquire access token for component")
                public WorkflowExecutionException call() throws Exception {
                    try {
                        final String accessToken =
                            acquireOrRegisterExecutionAuthorizationToken(wfDescriptionNode, distrCompKnowledge, resultMap);
                        synchronized (resultMap) {
                            // note: map key kept unchanged from previous code; could be improved/clarified
                            resultMap.put(wfDescriptionNode.getIdentifierAsObject().toString(), accessToken);
                        }
                        return null;
                    } catch (RemoteOperationException | OperationFailureException e) {
                        final String message = "Failed to acquire permission to execute component \"" + wfDescriptionNode.getName()
                            + "\" on " + wfDescriptionNode.getComponentDescription().getNode();
                        // log here immediately (without a stacktrace), as only the first exception will be rethrown
                        LOG.error(message + ": " + e.toString());
                        return new WorkflowExecutionException(message, e);
                    }
                }
            });

        }
        final List<WorkflowExecutionException> exceptions = callablesGroup.executeParallel(null);
        // rethrow first exception (if any) to abort
        for (WorkflowExecutionException e : exceptions) {
            if (e != null) {
                throw e;
            }
        }
        // synchronized to ensure thread visibility; may be redundant
        synchronized (resultMap) {
            return resultMap;
        }
    }

    private String acquireOrRegisterExecutionAuthorizationToken(WorkflowNode wfDescriptionNode,
        DistributedComponentKnowledge distrCompKnowledge, Map<String, String> resultMap)
        throws RemoteOperationException, OperationFailureException {

        // extract data
        ComponentDescription componentDescription = wfDescriptionNode.getComponentDescription();
        LogicalNodeId compLocation = componentDescription.getNode();
        String compIdWithoutVersion = componentDescription.getComponentInterface().getIdentifier();
        String compVersion = componentDescription.getComponentInterface().getVersion();

        final String accessToken;
        if (compLocation == null || platformService.matchesLocalInstance(compLocation)) {
            // for components on the same instance as the workflow initiator, special access is granted to the (potentially remote) workflow
            // controller; the id parameter is only provided here for usable log output
            accessToken = componentExecutionAuthorizationService
                .createAndRegisterExecutionTokenForLocalComponent(compIdWithoutVersion);
        } else {
            Optional<DistributedComponentEntry> distrComponentEntryResult =
                resolveComponentIdToDistributedComponentEntry(distrCompKnowledge, compLocation, compIdWithoutVersion);
            if (!distrComponentEntryResult.isPresent()) {
                throw new OperationFailureException("Could  not resolve component id " + compIdWithoutVersion
                    + " to an accessible component on instance " + compLocation.getAssociatedDisplayName());
            }
            DistributedComponentEntry distrComponentEntry = distrComponentEntryResult.get();

            AuthorizationPermissionSet matchingPermissionSet = distrComponentEntry.getMatchingPermissionSet();
            LOG.debug(StringUtils.format("Determined [%s] as the list of available authorization group(s) "
                + "for component '%s' on %s", matchingPermissionSet, compIdWithoutVersion, compLocation));

            RemotableComponentExecutionAuthorizationService remoteService =
                communicationService.getRemotableService(RemotableComponentExecutionAuthorizationService.class, compLocation);
            if (matchingPermissionSet.isPublic()) {
                accessToken = remoteService.requestExecutionTokenForPublicComponent(compIdWithoutVersion, compVersion);
            } else if (!matchingPermissionSet.isLocalOnly()) {
                // arbitrarily choose the first group shared by the local instance and the component host; no obvious criterion to choose by
                AuthorizationAccessGroup sharedAccessGroup = matchingPermissionSet.getAccessGroups().iterator().next();
                // request a token, which is returned encrypted with the group key (as a simple authorization check)
                String encryptedAccessToken = remoteService.requestEncryptedExecutionTokenViaGroupMembership(compIdWithoutVersion,
                    compVersion, sharedAccessGroup.getFullId());
                // attempt to decrypt it
                AuthorizationAccessGroupKeyData groupKeyData = authorizationService.getKeyDataForGroup(sharedAccessGroup);
                accessToken = cryptographyOperationsProvider.decodeAndDecryptString(groupKeyData.getSymmetricKey(), encryptedAccessToken);
                // simple verification: check for a known substring; must match the string composed within the token-generating method
                if (!accessToken.contains(":group:")) {
                    throw new OperationFailureException(
                        "Failed to decrypt the component execution token for component " + componentDescription.getName());
                }
            } else {
                throw new OperationFailureException(
                    "Failed to acquire permission to execute component \"" + componentDescription.getName()
                        + "\": There are no shared authorization groups between the local instance "
                        + "and the instance providing the component");
            }
        }
        return accessToken;
    }

    // TODO convert this into a central API method?
    private Optional<DistributedComponentEntry> resolveComponentIdToDistributedComponentEntry(
        DistributedComponentKnowledge distrCompKnowledge,
        LogicalNodeId compLocation, String compIdWithoutVersion) {
        for (DistributedComponentEntry componentEntry : distrCompKnowledge.getKnownSharedInstallationsOnNode(compLocation, false)) {
            if (componentEntry.getComponentInterface().getIdentifier().equals(compIdWithoutVersion)) {
                return Optional.of(componentEntry);
            }
        }
        return Optional.empty();
    }

    private void sendHeartbeatForActiveWorkflows() {
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

}

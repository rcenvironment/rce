/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.headless.api.ConsoleRowSubscriber;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.spi.SingleWorkflowStateChangeListener;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Default {@link HeadlessWorkflowExecutionService} implementation.
 * 
 * @author Sascha Zur
 * @author Phillip Kroll
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class HeadlessWorkflowExecutionServiceImpl implements HeadlessWorkflowExecutionService {

    private final Log log = LogFactory.getLog(getClass());

    private DistributedNotificationService notificationService;

    private WorkflowExecutionService workflowExecutionService;

    private DistributedComponentKnowledgeService compKnowledgeService;

    private PlatformService platformService;

    private MetaDataService metaDataService;

    @Override
    public void validatePlaceholdersFile(File placeholdersFile) throws WorkflowFileException {
        verifyFileExistsAndIsReadable(placeholdersFile);
        parsePlaceholdersFile(placeholdersFile);
    }

    @Override
    public FinalWorkflowState executeWorkflowSync(HeadlessWorkflowExecutionContext wfExeContext) throws WorkflowExecutionException {

        // create context
        final ExtendedHeadlessWorkflowExecutionContext headlessWfExeCtx = new ExtendedHeadlessWorkflowExecutionContext(wfExeContext);

        WorkflowState finalState = null; // = unknown

        final WorkflowDescription workflowDescription = loadWorkflowDescriptionAndPlaceholders(headlessWfExeCtx);

        headlessWfExeCtx.addOutput(null, StringUtils.format("'%s' starting...; log directory: %s (full path: %s)",
                headlessWfExeCtx.getWorkflowFile().getName(), headlessWfExeCtx.getLogDirectory().getAbsolutePath(),
                headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));

        try {
            executeWorkflow(workflowDescription, headlessWfExeCtx);
            headlessWfExeCtx.addOutput(headlessWfExeCtx.getWorkflowExecutionContext().getExecutionIdentifier(),
                StringUtils.format("'%s' executing...; id: %s (full path: %s)",
                    headlessWfExeCtx.getWorkflowFile().getName(),
                    headlessWfExeCtx.getWorkflowExecutionContext().getExecutionIdentifier(),
                    headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
            try {
                finalState = headlessWfExeCtx.waitForTermination();
            } catch (InterruptedException e) {
                throw new WorkflowExecutionException("Received interruption signal while waiting for workflow to terminate");
            }
        } catch (WorkflowExecutionException e) {
            headlessWfExeCtx.getTextOutputReceiver().addOutput(StringUtils.format("'%s' failed: %s (full path: %s) ",
                headlessWfExeCtx.getWorkflowFile().getName(), e.getMessage(), headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
            throw e;
        } finally {
            headlessWfExeCtx.closeResourcesQuietly();
            headlessWfExeCtx.unsubscribeNotificationSubscribersQuietly(notificationService);
        }
        headlessWfExeCtx.addOutput(StringUtils.format("%s: %s", headlessWfExeCtx.getWorkflowExecutionContext()
            .getExecutionIdentifier(), finalState.getDisplayName()), StringUtils.format("'%s' terminated: %s (full path: %s)",
                headlessWfExeCtx.getWorkflowFile().getName(), finalState.getDisplayName(),
                headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
        // map to reduced set of final workflow states (to avoid downstream checking for invalid
        // values)
        switch (finalState) {
        case FINISHED:
            return FinalWorkflowState.FINISHED;
        case CANCELLED:
            return FinalWorkflowState.CANCELLED;
        case FAILED:
            return FinalWorkflowState.FAILED;
        case UNKNOWN:
            throw new WorkflowExecutionException(StringUtils.format("Final state of '%s' is %s. "
                + "Most likely because the connection to the workflow host node was interupted. See logs for more details.",
                headlessWfExeCtx.getWorkflowFile().getAbsolutePath(), finalState.getDisplayName()));
        default:
            throw new WorkflowExecutionException(StringUtils.format("Unexpected value '%s' for final state for '%s'",
                finalState.getDisplayName(), headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
        }
    }

    private WorkflowDescription loadWorkflowDescriptionAndPlaceholders(ExtendedHeadlessWorkflowExecutionContext headlessWfExeCtx)
        throws WorkflowExecutionException {
        try {
            verifyFileExistsAndIsReadable(headlessWfExeCtx.getWorkflowFile());
            boolean abortIfWorkflowUpdateRequired = headlessWfExeCtx.shouldAbortIfWorkflowUpdateRequired();
            WorkflowDescription workflowDescription = loadWorkflowDescriptionFromFileConsideringUpdates(headlessWfExeCtx.getWorkflowFile(),
                new HeadlessWorkflowDescriptionLoaderCallback(headlessWfExeCtx.getTextOutputReceiver()), abortIfWorkflowUpdateRequired);
            if (headlessWfExeCtx.getPlaceholdersFile() != null) {
                verifyFileExistsAndIsReadable(headlessWfExeCtx.getPlaceholdersFile());
            }
            applyPlaceholdersAndVerify(workflowDescription, headlessWfExeCtx.getPlaceholdersFile());
            return workflowDescription;
        } catch (WorkflowFileException e) {
            throw new WorkflowExecutionException("Failed to execute workflow", e);
        }
    }

    private void executeWorkflow(final WorkflowDescription wfDescription, final ExtendedHeadlessWorkflowExecutionContext wfHeadlessExeCtx)
        throws WorkflowExecutionException {

        setupLogDirectory(wfHeadlessExeCtx);

        WorkflowExecutionUtils.replaceNullNodeIdentifiersWithActualNodeIdentifier(wfDescription, platformService.getLocalNodeId(),
            compKnowledgeService.getCurrentComponentKnowledge());
        WorkflowExecutionUtils.setNodeIdentifiersToTransientInCaseOfLocalOnes(wfDescription, platformService.getLocalNodeId());
        
        wfDescription.setName(WorkflowExecutionUtils.generateDefaultNameforExecutingWorkflow(wfHeadlessExeCtx.getWorkflowFile().getName(),
            wfDescription));
        wfDescription.setFileName(wfHeadlessExeCtx.getWorkflowFile().getName());

        if (!validateWorkflowDescription(wfDescription).isSucceeded()) {
            throw new WorkflowExecutionException("Workflow description invalid: " + wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath());
        }

        WorkflowExecutionContextBuilder wfExeCtxBuilder = new WorkflowExecutionContextBuilder(wfDescription);
        wfExeCtxBuilder.setInstanceName(wfDescription.getName());
        wfExeCtxBuilder.setNodeIdentifierStartedExecution(platformService.getLocalNodeId());
        if (wfDescription.getAdditionalInformation() != null && !wfDescription.getAdditionalInformation().isEmpty()) {
            wfExeCtxBuilder.setAdditionalInformationProvidedAtStart(wfDescription.getAdditionalInformation());
        }
        final WorkflowExecutionContext wfExeCtx = wfExeCtxBuilder.build();
        wfHeadlessExeCtx.setWorkflowExecutionContext(wfExeCtx);

        WorkflowStateNotificationSubscriber wfStateChangeListener = createWorkflowStateChangeListener(wfHeadlessExeCtx);

        // add workflow state subscriber; only subscribe for this specific workflow (no wildcard)
        try {
            ExtendedHeadlessWorkflowExecutionContext.NotificationSubscription subscriberContext =
                wfHeadlessExeCtx.new NotificationSubscription();
            subscriberContext.subscriber = wfStateChangeListener;
            subscriberContext.notificationId = WorkflowConstants.STATE_NOTIFICATION_ID + wfExeCtx.getExecutionIdentifier();
            subscriberContext.nodeId = wfExeCtx.getNodeId();
            notificationService.subscribe(subscriberContext.notificationId, subscriberContext.subscriber, subscriberContext.nodeId);
            wfHeadlessExeCtx.registerNotificationSubscriptionsToUnsubscribeOnFinish(subscriberContext);
        } catch (RemoteOperationException e) {
            String errorMessage = "Failed to execute workflow (error while subscribing for state changes)";
            log.error(StringUtils.format("%s: %s", errorMessage, wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath()), e);
            throw new WorkflowExecutionException(errorMessage, e);
        }

        // add console output subscriber
        ConsoleRowSubscriber consoleRowSubscriber = new ConsoleRowSubscriber(wfHeadlessExeCtx, wfHeadlessExeCtx.getLogDirectory());
        wfHeadlessExeCtx.registerResourceToCloseOnFinish(consoleRowSubscriber);

        // subscribe to a console row notification on workflow controller node
        try {
            ExtendedHeadlessWorkflowExecutionContext.NotificationSubscription subscriberContext =
                wfHeadlessExeCtx.new NotificationSubscription();
            subscriberContext.subscriber = consoleRowSubscriber;
            subscriberContext.notificationId = StringUtils.format("%s%s" + ConsoleRow.NOTIFICATION_SUFFIX,
                wfExeCtx.getExecutionIdentifier(), wfExeCtx.getNodeId().getIdString());
            subscriberContext.nodeId = wfExeCtx.getNodeId();
            notificationService.subscribe(subscriberContext.notificationId, subscriberContext.subscriber, subscriberContext.nodeId);
            wfHeadlessExeCtx.registerNotificationSubscriptionsToUnsubscribeOnFinish(subscriberContext);
        } catch (RemoteOperationException e) {
            log.error("Failed to subscribe for console row output: " + wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath(), e);
        }

        WorkflowExecutionInformation wfExeInfo;
        try {
            wfExeInfo = executeWorkflowAsync(wfExeCtx);
        } catch (RemoteOperationException e) {
            // consoleRowSubscriber is closed in calling method
            throw new WorkflowExecutionException("Failed to execute workflow", e);
        }

        log.debug(StringUtils.format("Created workflow from file '%s' with name '%s', with id %s on node %s",
            wfHeadlessExeCtx.getWorkflowFile().getName(), wfExeInfo.getInstanceName(), wfExeInfo.getExecutionIdentifier(),
            wfExeInfo.getNodeId()));
    }

    private WorkflowStateNotificationSubscriber createWorkflowStateChangeListener(
        final ExtendedHeadlessWorkflowExecutionContext wfHeadlessExeCtx) {
        final String wfExecutionId = wfHeadlessExeCtx.getWorkflowExecutionContext().getExecutionIdentifier();
        WorkflowStateNotificationSubscriber workflowStateChangeListener =
            new WorkflowStateNotificationSubscriber(new SingleWorkflowStateChangeListener() {

                @Override
                public void onWorkflowStateChanged(WorkflowState newState) {
                    log.debug(StringUtils.format("Received state change event for workflow '%s' (%s): %s",
                        wfHeadlessExeCtx.getWorkflowExecutionContext().getInstanceName(),
                        wfExecutionId, newState.getDisplayName()));
                    switch (newState) {
                    case CANCELLED:
                    case FAILED:
                    case FINISHED:
                        boolean getDisposed = wfHeadlessExeCtx.getDisposalBehavior() == DisposalBehavior.Always
                            || (newState == WorkflowState.FINISHED
                            && wfHeadlessExeCtx.getDisposalBehavior() == DisposalBehavior.OnFinished);
                        boolean getDeleted = wfHeadlessExeCtx.getDeletionBehavior() == DeletionBehavior.Always
                            || (newState == WorkflowState.FINISHED
                            && wfHeadlessExeCtx.getDeletionBehavior() == DeletionBehavior.OnFinished);
                        wfHeadlessExeCtx.reportWorkflowTerminated(newState, getDisposed);
                        if (getDeleted) {
                            try {
                                NodeIdentifier nodeId = wfHeadlessExeCtx.getWorkflowExecutionContext().getNodeId();
                                delete(workflowExecutionService.getWorkflowDataManagementId(wfExecutionId,
                                    nodeId), nodeId);
                                dispose(wfExecutionId, wfHeadlessExeCtx.getWorkflowExecutionContext().getNodeId());
                                // catching RTE might be obsolete/wrong after ECE was introduced
                            } catch (ExecutionControllerException | RemoteOperationException | RuntimeException e) {
                                log.error(StringUtils.format("Failed to delete workflow '%s' (%s) ",
                                    wfHeadlessExeCtx.getWorkflowExecutionContext().getInstanceName(), wfExecutionId), e);
                                wfHeadlessExeCtx.reportWorkflowDisposed(WorkflowState.FAILED);
                            }
                        } else {
                            if (getDisposed) {
                                try {
                                    dispose(wfExecutionId, wfHeadlessExeCtx.getWorkflowExecutionContext().getNodeId());
                                } catch (ExecutionControllerException | RemoteOperationException | RuntimeException e) {
                                    log.error(StringUtils.format("Failed to dispose workflow '%s' (%s) ",
                                        wfHeadlessExeCtx.getWorkflowExecutionContext().getInstanceName(), wfExecutionId), e);
                                    wfHeadlessExeCtx.reportWorkflowDisposed(WorkflowState.FAILED);
                                }
                            }
                        }
                        break;
                    case DISPOSED:
                        wfHeadlessExeCtx.reportWorkflowDisposed(newState);
                        break;
                    default:
                        // ignore
                        break; // workaround for CheckStyle bug
                               // (http://sourceforge.net/p/checkstyle/bugs/454/) - misc_ro
                    }
                }

                @Override
                public void onWorkflowNotAliveAnymore(String errorMessage) {
                    wfHeadlessExeCtx.reportWorkflowNotAliveAnymore(errorMessage);
                }

            }, wfExecutionId);
        return workflowStateChangeListener;
    }

    private void applyPlaceholdersAndVerify(final WorkflowDescription workflowDescription,
        final File placeholdersFile) throws WorkflowExecutionException, WorkflowFileException {

        final Map<String, Map<String, String>> placeholderValues;
        if (placeholdersFile != null) {
            placeholderValues = parsePlaceholdersFile(placeholdersFile);
            log.debug(StringUtils.format("Loaded placeholder values from %s: %s", placeholdersFile.getAbsolutePath(), placeholderValues));
        } else {
            placeholderValues = new HashMap<>();
        }

        WorkflowPlaceholderHandler placeholderDescription =
            WorkflowPlaceholderHandler.createPlaceholderDescriptionFromWorkflowDescription(workflowDescription, "");

        // check for unsupported placeholders
        Map<String, Map<String, String>> componentTypePlaceholders = placeholderDescription.getComponentTypePlaceholders();
        if (!componentTypePlaceholders.isEmpty()) {
            throw new WorkflowExecutionException(
                "This workflow uses component *type* placeholders which are not supported in headless execution yet");
        }

        // get copyInstanceId -> (key->value) map of placeholders
        Map<String, Map<String, String>> componentInstancePlaceholders = placeholderDescription.getComponentInstancePlaceholders();

        Set<String> missingPlaceholderValues = new TreeSet<>();

        // collect required placeholder values
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            if (wn.isEnabled()) {
                // extract information
                String compInstanceId = wn.getIdentifier();
                String componentId = wn.getComponentDescription().getIdentifier();
                // first, try component instance specific placeholders definition
                Map<String, String> ciPlaceholderValues = placeholderValues.get(createComponentInstancePlaceholderKey(wn));
                // then, try component type specific placeholders definition
                if (ciPlaceholderValues == null) {
                    ciPlaceholderValues = placeholderValues.get(componentId);
                }
                Map<String, String> ciPlaceholders = componentInstancePlaceholders.get(compInstanceId);
                // subtract available keys and collect remaining (missing) ones
                if (ciPlaceholders != null) {
                    Set<String> ciPlaceholderKeys = ciPlaceholders.keySet();
                    Set<String> missingCIPlaceholderKeys = ciPlaceholderKeys;
                    if (ciPlaceholderValues != null) {
                        missingCIPlaceholderKeys.removeAll(ciPlaceholderValues.keySet());
                    }

                    // explicit fixes for backwards compatibility; ignore placeholders in settings that
                    // are not actually used
                    eliminateKnownIrrelevantPlaceholders(wn, missingCIPlaceholderKeys);

                    for (String missingKey : missingCIPlaceholderKeys) {
                        missingPlaceholderValues.add(StringUtils.format("\"%s\" -> \"%s\" (%s)", componentId, missingKey, compInstanceId));
                    }
                }
                // apply available values
                if (ciPlaceholderValues != null && !ciPlaceholderValues.isEmpty()) {
                    logPlaceholderValues(wn, ciPlaceholderValues);
                    wn.getComponentDescription().getConfigurationDescription().setPlaceholders(ciPlaceholderValues);
                }
            }
        }

        // check if missing set contains entries -> fail if it does
        if (!missingPlaceholderValues.isEmpty()) {
            throw new WorkflowExecutionException("The workflow requires additional placeholder values "
                + "(listed as <component id>/<version> -> <placeholder key> (<instance id>)): " + missingPlaceholderValues);
        }
    }

    private void setupLogDirectory(ExtendedHeadlessWorkflowExecutionContext wfHeadlessExeCtx) throws WorkflowExecutionException {
        File logDirectory = wfHeadlessExeCtx.getLogDirectory();
        logDirectory.mkdirs();
        if (!logDirectory.isDirectory()) {
            throw new WorkflowExecutionException(StringUtils.format("Failed to create log directory '%s' for workflow '%s'"
                + logDirectory.getAbsolutePath(), wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath()));
        }
        log.debug(StringUtils.format("Writing log files for workflow '%s' to: ",
            wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath(), logDirectory.getAbsolutePath()));
    }

    private String createComponentInstancePlaceholderKey(WorkflowNode wn) {
        return wn.getComponentDescription().getIdentifier() + "/" + wn.getName();
    }

    private void logPlaceholderValues(WorkflowNode wn, Map<String, String> cPlaceholderValues) {
        PlaceholdersMetaDataDefinition placeholderMetaDataDefinition = wn.getComponentDescription().getConfigurationDescription()
            .getComponentConfigurationDefinition().getPlaceholderMetaDataDefinition();

        Map<String, String> cPlaceholderValuesToLog = new HashMap<>();
        for (String cPlaceholderKey : cPlaceholderValues.keySet()) {
            if (placeholderMetaDataDefinition.decode(cPlaceholderKey)) {
                cPlaceholderValuesToLog.put(cPlaceholderKey, "*****");
            } else {
                cPlaceholderValuesToLog.put(cPlaceholderKey, cPlaceholderValues.get(cPlaceholderKey));
            }
        }
        log.debug(StringUtils.format("Applying %d placeholder value(s) to workflow node %s: %s", cPlaceholderValues.size(), wn,
            cPlaceholderValuesToLog));
    }

    private void eliminateKnownIrrelevantPlaceholders(WorkflowNode wn, Set<String> missingCIPlaceholderKeys) {
        Set<String> activeConfigKeys = wn.getComponentDescription().getConfigurationDescription()
            .getActiveConfigurationDefinition().getConfigurationKeys();

        Iterator<String> phKeysIterator = missingCIPlaceholderKeys.iterator();
        while (phKeysIterator.hasNext()) {
            String phKey = phKeysIterator.next();
            if (!activeConfigKeys.contains(phKey)) {
                phKeysIterator.remove();
            }
        }
    }

    /**
     * Helper function, detects the workflow information for a given executionId.
     * 
     */
    private WorkflowExecutionInformation getWfExecInfFromExecutionId(String executionId) {

        WorkflowExecutionInformation wExecInf = null;
        Set<WorkflowExecutionInformation> wis = workflowExecutionService.getWorkflowExecutionInformations();
        for (WorkflowExecutionInformation workflow : wis) {
            if (workflow.getExecutionIdentifier().equals(executionId)) {
                wExecInf = workflow;
                break;
            }
        }
        if (wExecInf == null) {
            log.error("Workflow with id '" + executionId + "' not found");
        }
        return wExecInf;
    }

    private Map<String, Map<String, String>> parsePlaceholdersFile(File placeholdersFile) throws WorkflowFileException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            return mapper.readValue(placeholdersFile, new TypeReference<HashMap<String, HashMap<String, String>>>() {
            });
        } catch (IOException e) {
            throw new WorkflowFileException(StringUtils.format("Failed to parse placeholders file: %s",
                placeholdersFile.getAbsolutePath()), e);
        }
    }

    private void verifyFileExistsAndIsReadable(File file) throws WorkflowFileException {
        if (!file.isFile()) {
            throw new WorkflowFileException(StringUtils.format("File doesn't exis: %s", file.getAbsolutePath()));
        }
        if (!file.canRead()) {
            throw new WorkflowFileException(StringUtils.format("File can not be read: %s", file.getAbsolutePath()));
        }
    }

    @Override
    public WorkflowDescriptionValidationResult validateWorkflowDescription(WorkflowDescription workflowDescription) {
        return workflowExecutionService.validateWorkflowDescription(workflowDescription);
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException {
        return workflowExecutionService.loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, callback);
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback,
        boolean abortIfWorkflowUpdateRequired) throws WorkflowFileException {
        return workflowExecutionService.loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, callback, abortIfWorkflowUpdateRequired);
    }

    @Override
    public WorkflowDescription loadWorkflowDescriptionFromFile(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException {
        return workflowExecutionService.loadWorkflowDescriptionFromFile(wfFile, callback);
    }

    @Override
    public WorkflowExecutionInformation executeWorkflowAsync(WorkflowExecutionContext executionContext) throws WorkflowExecutionException,
        RemoteOperationException {
        return workflowExecutionService.executeWorkflowAsync(executionContext);
    }

    @Override
    public void cancel(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.cancel(executionId, node);
    }

    @Override
    public void pause(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.pause(executionId, node);
    }

    @Override
    public void resume(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.resume(executionId, node);
    }

    @Override
    public void dispose(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.dispose(executionId, node);
    }

    /**
     * Delete workflow run from data management.
     * 
     * @param wfId to delete
     * @param node to delete from
     */
    public void delete(Long wfId, NodeIdentifier node) {
        try {
            metaDataService.deleteWorkflowRun(wfId, node);
        } catch (CommunicationException e) {
            log.error("Could not delete worklflow run " + wfId);
        }
    }

    @Override
    public WorkflowState getWorkflowState(String executionId, NodeIdentifier node) throws ExecutionControllerException,
        RemoteOperationException {
        return workflowExecutionService.getWorkflowState(executionId, node);
    }

    @Override
    public Long getWorkflowDataManagementId(String executionId, NodeIdentifier node) throws ExecutionControllerException,
        RemoteOperationException {
        return workflowExecutionService.getWorkflowDataManagementId(executionId, node);
    }

    @Override
    public Set<WorkflowExecutionInformation> getLocalWorkflowExecutionInformations() {
        return workflowExecutionService.getLocalWorkflowExecutionInformations();
    }

    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations() {
        return workflowExecutionService.getWorkflowExecutionInformations();
    }

    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations(boolean forceRefresh) {
        return workflowExecutionService.getWorkflowExecutionInformations(forceRefresh);
    }

    /**
     * OSGi injection method. For test purposes set to public.
     * 
     * @param newService {@link DistributedNotificationService} instance
     */
    public void bindDistributedNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }

    /**
     * OSGi injection method. For test purposes set to public.
     * 
     * @param newService {@link WorkflowExecutionService} instance
     */
    public void bindWorkflowExecutionService(WorkflowExecutionService newService) {
        workflowExecutionService = newService;
    }

    /**
     * OSGi injection method. For test purposes set to public.
     * 
     * @param newService {@link PlatformService} instance
     */
    public void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        compKnowledgeService = newInstance;
    }

    protected void bindMetaDataService(MetaDataService incoming) {
        this.metaDataService = incoming;
    }

}

/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
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
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Default {@link HeadlessWorkflowExecutionService} implementation.
 * 
 * Note: Implementation of headless workflow execution is eroded for different reasons. Root cause is that it was introduce without a
 * reliable concept but just by start implementing. The code is also a bit messed up as {@link NotificationSubscriber}s must be implemented
 * to recognize certain workflow states. I would suggest to start over at some point in time. --seid_do
 * 
 * @author Sascha Zur
 * @author Phillip Kroll
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Brigitte Boden
 */
@Component
public class HeadlessWorkflowExecutionServiceImpl implements HeadlessWorkflowExecutionService {

    private DistributedNotificationService notificationService;

    private WorkflowExecutionService workflowExecutionService;

    private DistributedComponentKnowledgeService compKnowledgeService;

    private PlatformService platformService;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void validatePlaceholdersFile(File placeholdersFile) throws WorkflowFileException {
        verifyFileExistsAndIsReadable(placeholdersFile);
        parsePlaceholdersFile(placeholdersFile);
    }

    @Override
    public FinalWorkflowState executeWorkflow(ExtendedHeadlessWorkflowExecutionContext wfExeContext) throws WorkflowExecutionException {
        startHeadlessWorkflowExecution(wfExeContext);
        return waitForWorkflowTerminationAndCleanup(wfExeContext);
    }

    @Override
    public WorkflowExecutionInformation startHeadlessWorkflowExecution(HeadlessWorkflowExecutionContext wfExeContext)
        throws WorkflowExecutionException {
        ExtendedHeadlessWorkflowExecutionContext headlessWfExeCtx = (ExtendedHeadlessWorkflowExecutionContext) wfExeContext;
        return startHeadlessWorkflowExecutionInternal1(headlessWfExeCtx);
    }

    @Override
    public FinalWorkflowState waitForWorkflowTerminationAndCleanup(HeadlessWorkflowExecutionContext wfExeContext)
        throws WorkflowExecutionException {
        ExtendedHeadlessWorkflowExecutionContext headlessWfExeCtx = (ExtendedHeadlessWorkflowExecutionContext) wfExeContext;
        FinalWorkflowState finalState = waitForWorkflowExecutionTermination(headlessWfExeCtx);
        disposeOrDeleteWorkflowIfIntended(headlessWfExeCtx, finalState.equals(FinalWorkflowState.FINISHED));
        headlessWfExeCtx.unsubscribeNotificationSubscribersQuietly(notificationService);
        return finalState;
    }

    private WorkflowExecutionInformation startHeadlessWorkflowExecutionInternal1(ExtendedHeadlessWorkflowExecutionContext headlessWfExeCtx)
        throws WorkflowExecutionException {
        headlessWfExeCtx.addOutput(null, StringUtils.format("Loading: '%s'; log directory: %s (full path: %s)",
            headlessWfExeCtx.getWorkflowFile().getName(), headlessWfExeCtx.getLogDirectory().getAbsolutePath(),
            headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));

        final WorkflowDescription workflowDescription = loadWorkflowDescriptionAndPlaceholders(headlessWfExeCtx);

        WorkflowExecutionInformation wfExecInfo;
        try {
            wfExecInfo = startHeadlessWorkflowExecutionInternal2(workflowDescription, headlessWfExeCtx);
            headlessWfExeCtx.addOutput(headlessWfExeCtx.getWorkflowExecutionContext().getExecutionIdentifier(),
                StringUtils.format("Executing: '%s'; id: %s (full path: %s)",
                    headlessWfExeCtx.getWorkflowFile().getName(),
                    headlessWfExeCtx.getWorkflowExecutionContext().getExecutionIdentifier(),
                    headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
        } catch (WorkflowExecutionException e) {
            headlessWfExeCtx.getTextOutputReceiver().addOutput(StringUtils.format("Failed: '%s'; %s (full path: %s) ",
                headlessWfExeCtx.getWorkflowFile().getName(), e.getMessage(), headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
            headlessWfExeCtx.closeResourcesQuietly();
            headlessWfExeCtx.unsubscribeNotificationSubscribersQuietly(notificationService);
            throw e;
        }
        return wfExecInfo;
    }

    private FinalWorkflowState waitForWorkflowExecutionTermination(ExtendedHeadlessWorkflowExecutionContext headlessWfExeCtx)
        throws WorkflowExecutionException {
        WorkflowState finalState = null; // = unknown
        try {
            finalState = headlessWfExeCtx.waitForTermination();
        } catch (InterruptedException e) {
            throw new WorkflowExecutionException("Received interruption signal while waiting for workflow to terminate");
        }
        headlessWfExeCtx.addOutput(StringUtils.format("%s: %s", headlessWfExeCtx.getWorkflowExecutionContext()
            .getExecutionIdentifier(), finalState.getDisplayName()), StringUtils.format("%s: '%s'(full path: %s)",
                finalState.getDisplayName(), headlessWfExeCtx.getWorkflowFile().getName(),
                headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
        headlessWfExeCtx.closeResourcesQuietly();
        // map to reduced set of final workflow states (to avoid downstream checking for invalid values)
        switch (finalState) {
        case FINISHED:
            return FinalWorkflowState.FINISHED;
        case CANCELLED:
            return FinalWorkflowState.CANCELLED;
        case FAILED:
            return FinalWorkflowState.FAILED;
        case RESULTS_REJECTED:
            return FinalWorkflowState.RESULTS_REJECTED;
        case UNKNOWN:
            throw new WorkflowExecutionException(StringUtils.format("Final state of '%s' is %s. "
                + "Most likely because the connection to the workflow host node was interupted. See logs for more details.",
                headlessWfExeCtx.getWorkflowFile().getAbsolutePath(), finalState.getDisplayName()));
        default:
            throw new WorkflowExecutionException(StringUtils.format("Unexpected value '%s' for final state for '%s'",
                finalState.getDisplayName(), headlessWfExeCtx.getWorkflowFile().getAbsolutePath()));
        }
    }

    private void afterWorkflowExecutionTerminated(ExtendedHeadlessWorkflowExecutionContext headlessWfExeCtx, boolean behavedAsExpected)
        throws WorkflowExecutionException {
        disposeOrDeleteWorkflowIfIntended(headlessWfExeCtx, behavedAsExpected);
        headlessWfExeCtx.unsubscribeNotificationSubscribersQuietly(notificationService);
    }

    private void disposeOrDeleteWorkflowIfIntended(ExtendedHeadlessWorkflowExecutionContext wfHeadlessExeCtx, boolean behavedAsExpected) {
        final String wfExecutionId = wfHeadlessExeCtx.getWorkflowExecutionContext().getExecutionIdentifier();
        final WorkflowExecutionHandle wfExecutionHandle = wfHeadlessExeCtx.getWorkflowExecutionContext().getWorkflowExecutionHandle();

        boolean dispose = wfHeadlessExeCtx.getDisposalBehavior() == DisposalBehavior.Always
            || (behavedAsExpected
                && wfHeadlessExeCtx.getDisposalBehavior() == DisposalBehavior.OnExpected);
        boolean delete = wfHeadlessExeCtx.getDeletionBehavior() == DeletionBehavior.Always
            || (behavedAsExpected
                && wfHeadlessExeCtx.getDeletionBehavior() == DeletionBehavior.OnExpected);
        if (delete) { // includes disposal
            try {
                deleteFromDataManagement(wfExecutionHandle);
                dispose(wfExecutionHandle);
                wfHeadlessExeCtx.waitForDisposal();
                try {
                    FileUtils.deleteDirectory(wfHeadlessExeCtx.getLogDirectory());
                } catch (IOException e) {
                    log.error("Failed to delete log directory: " + wfHeadlessExeCtx.getLogDirectory(), e);
                }
                // catching RTE might be obsolete/wrong after ECE was introduced
            } catch (ExecutionControllerException | RemoteOperationException | RuntimeException e) {
                log.error(StringUtils.format("Failed to delete workflow '%s' (%s) ",
                    wfHeadlessExeCtx.getWorkflowExecutionContext().getInstanceName(), wfExecutionId), e);
                wfHeadlessExeCtx.reportWorkflowDisposed(WorkflowState.FAILED);
            } catch (InterruptedException e) {
                log.error(StringUtils.format("Received interruption signal while waiting for disposeal of workflow '%s' (%s) ",
                    wfHeadlessExeCtx.getWorkflowExecutionContext().getInstanceName(), wfExecutionId), e);
            }
        } else {
            if (dispose) {
                try {
                    dispose(wfExecutionHandle);
                    wfHeadlessExeCtx.waitForDisposal();
                } catch (ExecutionControllerException | RemoteOperationException | RuntimeException e) {
                    log.error(StringUtils.format("Failed to dispose workflow '%s' (%s) ",
                        wfHeadlessExeCtx.getWorkflowExecutionContext().getInstanceName(), wfExecutionId), e);
                } catch (InterruptedException e) {
                    log.error(StringUtils.format("Received interruption signal while waiting for disposeal of workflow '%s' (%s) ",
                        wfHeadlessExeCtx.getWorkflowExecutionContext().getInstanceName(), wfExecutionId), e);
                }
            }
        }

    }

    @Override
    public HeadlessWorkflowExecutionVerificationResult executeWorkflowsAndVerify(Set<HeadlessWorkflowExecutionContext> headlessWfExeCtxs,
        HeadlessWorkflowExecutionVerificationRecorder wfVerificationResultReorder) {

        Set<ExtendedHeadlessWorkflowExecutionContext> extHeadlessWfExeCtxs = new HashSet<>();

        Set<ExtendedHeadlessWorkflowExecutionContext> extHeadlessWfExeCtxsExpected = new HashSet<>();

        for (HeadlessWorkflowExecutionContext headlessWfExeCtx : headlessWfExeCtxs) {
            final ExtendedHeadlessWorkflowExecutionContext extHeadlessWfExeCtx = new ExtendedHeadlessWorkflowExecutionContext();
            extHeadlessWfExeCtx.setWfFile(headlessWfExeCtx.getWorkflowFile());
            extHeadlessWfExeCtx.setLogDirectory(headlessWfExeCtx.getLogDirectory());
            extHeadlessWfExeCtx.setPlaceholdersFile(headlessWfExeCtx.getPlaceholdersFile());
            extHeadlessWfExeCtx.setTextOutputReceiver(headlessWfExeCtx.getTextOutputReceiver());
            extHeadlessWfExeCtx.setIsCompactOutput(headlessWfExeCtx.isCompactOutput());
            extHeadlessWfExeCtx.setSingleConsoleRowsProcessor(headlessWfExeCtx.getSingleConsoleRowReceiver());
            extHeadlessWfExeCtx.setDisposeBehavior(headlessWfExeCtx.getDisposalBehavior());
            extHeadlessWfExeCtx.setDeletionBehavior(headlessWfExeCtx.getDeletionBehavior());
            extHeadlessWfExeCtx.setAbortIfWorkflowUpdateRequired(headlessWfExeCtx.shouldAbortIfWorkflowUpdateRequired());
            try {
                startHeadlessWorkflowExecutionInternal1(extHeadlessWfExeCtx);
            } catch (WorkflowExecutionException e) {
                wfVerificationResultReorder.addWorkflowError(headlessWfExeCtx.getWorkflowFile(), e.getMessage());
                log.error(e.getMessage(), e);
                continue;
            }
            extHeadlessWfExeCtxs.add(extHeadlessWfExeCtx);
        }

        for (ExtendedHeadlessWorkflowExecutionContext extHeadlessWfExeCtx : extHeadlessWfExeCtxs) {
            try {
                FinalWorkflowState finalState = waitForWorkflowExecutionTermination(extHeadlessWfExeCtx);
                boolean behavedAsExpected = false;
                try {
                    behavedAsExpected = wfVerificationResultReorder.addWorkflowExecutionResult(extHeadlessWfExeCtx.getWorkflowFile(),
                        extHeadlessWfExeCtx.getLogFiles(), finalState, extHeadlessWfExeCtx.getExecutionDuration());
                } catch (IOException e) {
                    wfVerificationResultReorder.addWorkflowError(extHeadlessWfExeCtx.getWorkflowFile(), e.getMessage());
                }
                if (behavedAsExpected) {
                    extHeadlessWfExeCtxsExpected.add(extHeadlessWfExeCtx);
                }
                afterWorkflowExecutionTerminated(extHeadlessWfExeCtx, behavedAsExpected);
            } catch (WorkflowExecutionException e) {
                wfVerificationResultReorder.addWorkflowError(extHeadlessWfExeCtx.getWorkflowFile(), e.getMessage());
            }
        }

        return (HeadlessWorkflowExecutionVerificationResult) wfVerificationResultReorder;
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

    private WorkflowExecutionInformation startHeadlessWorkflowExecutionInternal2(final WorkflowDescription wfDescription,
        final ExtendedHeadlessWorkflowExecutionContext wfHeadlessExeCtx)
        throws WorkflowExecutionException {

        setupLogDirectory(wfHeadlessExeCtx);

        WorkflowExecutionUtils.replaceNullNodeIdentifiersWithActualNodeIdentifier(wfDescription,
            platformService.getLocalDefaultLogicalNodeId(), compKnowledgeService.getCurrentSnapshot());
        WorkflowExecutionUtils
            .setNodeIdentifiersToTransientInCaseOfLocalOnes(wfDescription, platformService.getLocalDefaultLogicalNodeId());

        wfDescription.setName(WorkflowExecutionUtils.generateDefaultNameforExecutingWorkflow(wfHeadlessExeCtx.getWorkflowFile().getName(),
            wfDescription));
        wfDescription.setFileName(wfHeadlessExeCtx.getWorkflowFile().getName());

        if (!validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(wfDescription).isSucceeded()) {
            throw new WorkflowExecutionException("Workflow description invalid: " + wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath());
        }

        WorkflowExecutionContextBuilder wfExeCtxBuilder = new WorkflowExecutionContextBuilder(wfDescription);
        wfExeCtxBuilder.setInstanceName(wfDescription.getName());
        wfExeCtxBuilder.setNodeIdentifierStartedExecution(platformService.getLocalDefaultLogicalNodeId());
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
        insertLogFileMetaInformation(consoleRowSubscriber, wfExeCtx);
        wfHeadlessExeCtx.registerResourceToCloseOnFinish(consoleRowSubscriber);

        // subscribe to a console row notification on workflow controller node
        try {
            ExtendedHeadlessWorkflowExecutionContext.NotificationSubscription subscriberContext =
                wfHeadlessExeCtx.new NotificationSubscription();
            subscriberContext.subscriber = consoleRowSubscriber;
            subscriberContext.notificationId = ConsoleRowUtils.composeConsoleNotificationId(wfExeCtx.getNodeId(),
                wfExeCtx.getExecutionIdentifier());
            subscriberContext.nodeId = wfExeCtx.getNodeId();
            notificationService.subscribe(subscriberContext.notificationId, subscriberContext.subscriber, subscriberContext.nodeId);
            wfHeadlessExeCtx.registerNotificationSubscriptionsToUnsubscribeOnFinish(subscriberContext);
        } catch (RemoteOperationException e) {
            log.error("Failed to subscribe for console row output: " + wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath(), e);
        }

        WorkflowExecutionInformation wfExeInfo;
        try {
            wfExeInfo = startWorkflowExecution(wfExeCtx);
        } catch (RemoteOperationException e) {
            // consoleRowSubscriber is closed in calling method
            throw new WorkflowExecutionException("Failed to execute workflow", e);
        }

        log.debug(StringUtils.format("Created workflow from file '%s' with name '%s', with id %s on node %s",
            wfHeadlessExeCtx.getWorkflowFile().getName(), wfExeInfo.getInstanceName(), wfExeInfo.getExecutionIdentifier(),
            wfExeInfo.getNodeId()));
        return wfExeInfo;
    }

    /**
     * Writes a log file header providing information about log file formation version, wf and component controller locations, and the node
     * that initiated the workflow run.
     */
    private void insertLogFileMetaInformation(ConsoleRowSubscriber consoleRowSubscriber, final WorkflowExecutionContext wfExeCtx) {
        consoleRowSubscriber.insertMetaInformation("Log file format 1.1");
        final WorkflowDescription workflowDescription = wfExeCtx.getWorkflowDescription();
        consoleRowSubscriber.insertMetaInformation("Workflow run initiated from instance " + wfExeCtx.getNodeIdStartedExecution());
        consoleRowSubscriber.insertMetaInformation("Location of workflow controller: " + workflowDescription.getControllerNode());
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            consoleRowSubscriber.insertMetaInformation(
                StringUtils.format("Location of workflow component \"%s\" [%s]: %s", wfNode.getName(), wfNode.getIdentifier(),
                    wfNode.getComponentDescription().getNode()));
        }
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
                    case RESULTS_REJECTED:
                    case FINISHED:
                        wfHeadlessExeCtx.reportWorkflowTerminated(newState);
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
        if (logDirectory.isDirectory()) {
            log.debug(StringUtils.format("Created log file directory '%s' for execution of '%s'",
                logDirectory.getAbsolutePath(), wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath()));
        } else {
            throw new WorkflowExecutionException(StringUtils.format("Failed to create log directory '%s' for execution of '%s'",
                logDirectory.getAbsolutePath(), wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath()));
        }
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

        Iterator<String> phKeysIterator = missingCIPlaceholderKeys.iterator();
        while (phKeysIterator.hasNext()) {
            String phKey = phKeysIterator.next();
            // Check if the key is active
            if (!WorkflowPlaceholderHandler.isActivePlaceholder(phKey, wn.getComponentDescription().getConfigurationDescription())) {
                phKeysIterator.remove();
            }
        }
    }

    private Map<String, Map<String, String>> parsePlaceholdersFile(File placeholdersFile) throws WorkflowFileException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            return mapper.readValue(placeholdersFile, new TypeReference<HashMap<String, Map<String, String>>>() {
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
    public WorkflowDescriptionValidationResult validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(
        WorkflowDescription workflowDescription) {
        return workflowExecutionService.validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(workflowDescription);
    }

    @Override
    public Map<String, String> validateRemoteWorkflowControllerVisibilityOfComponents(WorkflowDescription wfDescription) {
        return workflowExecutionService.validateRemoteWorkflowControllerVisibilityOfComponents(wfDescription);
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
    public WorkflowExecutionInformation startWorkflowExecution(WorkflowExecutionContext executionContext) throws WorkflowExecutionException,
        RemoteOperationException {
        return workflowExecutionService.startWorkflowExecution(executionContext);
    }

    @Override
    public void cancel(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.cancel(handle);
    }

    @Override
    public void pause(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.pause(handle);
    }

    @Override
    public void resume(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.resume(handle);
    }

    @Override
    public void dispose(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        workflowExecutionService.dispose(handle);
    }

    @Override
    public void deleteFromDataManagement(WorkflowExecutionHandle handle) throws ExecutionControllerException {
        workflowExecutionService.deleteFromDataManagement(handle);
    }

    @Override
    public WorkflowState getWorkflowState(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        return workflowExecutionService.getWorkflowState(handle);
    }

    @Override
    public Long getWorkflowDataManagementId(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        return workflowExecutionService.getWorkflowDataManagementId(handle);
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
     * OSGi injection method. Made public for access by test code.
     * 
     * @param newService {@link DistributedNotificationService} instance
     */
    @Reference
    public void bindDistributedNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }

    /**
     * OSGi injection method. Made public for access by test code.
     * 
     * @param newService {@link WorkflowExecutionService} instance
     */
    @Reference
    public void bindWorkflowExecutionService(WorkflowExecutionService newService) {
        workflowExecutionService = newService;
    }

    /**
     * OSGi injection method. Made public for access by test code.
     * 
     * @param newService {@link PlatformService} instance
     */
    @Reference
    public void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    /**
     * OSGi injection method. Made public for access by test code.
     * 
     * @param newService {@link DistributedComponentKnowledgeService} instance
     */
    @Reference
    public void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        compKnowledgeService = newService;
    }

}

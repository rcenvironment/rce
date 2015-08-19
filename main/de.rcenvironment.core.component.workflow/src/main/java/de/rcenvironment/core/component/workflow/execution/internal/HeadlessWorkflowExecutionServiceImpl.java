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
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidator;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.impl.ConsoleRowSubscriber;
import de.rcenvironment.core.component.workflow.execution.impl.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowStateChangeListener;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateService;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateUtils;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Default {@link HeadlessWorkflowExecutionService} implementation.
 * 
 * @author Sascha Zur
 * @author Phillip Kroll
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class HeadlessWorkflowExecutionServiceImpl implements HeadlessWorkflowExecutionService {

    private static final String FILE_TYPE_PLACEHOLDER = "placeholder";

    private static final String FILE_TYPE_WORKFLOW = "workflow";

    private NodeIdentifier localNode;

    private DistributedNotificationService distributedNotificationService;

    private WorkflowExecutionService workflowExecutionService;

    private DistributedComponentKnowledgeService compKnowledgeService;

    private PersistentWorkflowDescriptionUpdateService persistentWorkflowDescriptionUpdateService;

    private PlatformService platformService;

    private final Log log = LogFactory.getLog(getClass());

    private WorkflowHostService workflowHostService;

    /**
     * Default constructor; should only be used by OSGi-DS and unit tests.
     */
    public HeadlessWorkflowExecutionServiceImpl() {
    }

    @Override
    public WorkflowDescription parseWorkflowFile(File wfFile, final TextOutputReceiver outputReceiver) throws WorkflowExecutionException {
        if (!wfFile.isFile()) {
            verifyIsReadableFile(wfFile, FILE_TYPE_WORKFLOW);
        }

        // create context
        final HeadlessWorkflowExecutionContext context = new HeadlessWorkflowExecutionContext(wfFile, outputReceiver, null);

        return loadWorkflowDescription(context);
    }

    @Override
    public boolean isWorkflowDescriptionValid(WorkflowDescription workflowDescription) {
        NodeIdentifier controllerNode = workflowDescription.getControllerNode();
        if (controllerNode == null) {
            controllerNode = platformService.getLocalNodeId();
        }
        if (!workflowHostService.getWorkflowHostNodesAndSelf().contains(controllerNode)) {
            log.warn("Configured controller node is not available: " + controllerNode);
            return false;
        }

        DistributedComponentKnowledge compKnowledge = compKnowledgeService.getCurrentComponentKnowledge();

        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            NodeIdentifier componentNode = node.getComponentDescription().getNode();
            if (componentNode == null) {
                componentNode = platformService.getLocalNodeId();
            }
            if (!ComponentUtils.hasComponent(compKnowledge.getAllInstallations(), node.getComponentDescription().getIdentifier(),
                componentNode)) {
                log.warn(String.format("Component '%s' not installed on configured component node '%s' or node is not available",
                    node.getComponentDescription().getName(), componentNode));
                return false;
            }
        }
        return true;
    }

    @Override
    public void validatePlaceholdersFile(File placeholdersFile) throws WorkflowExecutionException {
        verifyIsReadableFile(placeholdersFile, FILE_TYPE_PLACEHOLDER);
        readPlaceholdersFile(placeholdersFile);
    }
    
    @Override
    public FinalWorkflowState executeWorkflow(File wfFile, File placeholdersFile, File customLogDirectory,
        TextOutputReceiver outputReceiver, SingleConsoleRowsProcessor customConsoleRowReceiver) throws WorkflowExecutionException {
        return executeWorkflow(wfFile, placeholdersFile, customLogDirectory, outputReceiver, customConsoleRowReceiver, Dispose.OnFinished);
    }

    @Override
    public FinalWorkflowState executeWorkflow(File wfFile, File placeholdersFile, File logDirectory,
        final TextOutputReceiver outputReceiver, final SingleConsoleRowsProcessor customConsoleRowsProcessor, Dispose dispose)
        throws WorkflowExecutionException {

        WorkflowState finalState = null; // = unknown

        verifyIsReadableFile(wfFile, FILE_TYPE_WORKFLOW);

        // create context
        final HeadlessWorkflowExecutionContext headlessWfExeCtx = new HeadlessWorkflowExecutionContext(wfFile,
            outputReceiver, customConsoleRowsProcessor);
        headlessWfExeCtx.setDispose(dispose);

        // parse workflow file
        final WorkflowDescription workflowDescription = loadWorkflowDescription(headlessWfExeCtx);

        applyPlaceholdersAndVerify(workflowDescription, placeholdersFile);

        try {
            if (logDirectory == null) {
                throw new IllegalArgumentException("Log directory can not be null");
            }
            headlessWfExeCtx.setLogDirectory(logDirectory);
            // run
            headlessWfExeCtx.addOutput(String.format("Starting execution of workflow '%s' using log directory '%s'",
                wfFile.getAbsolutePath(), logDirectory.getAbsolutePath()));
            try {
                startWorkflowExecution(workflowDescription, headlessWfExeCtx);
                finalState = headlessWfExeCtx.waitForTermination();
            } catch (InterruptedException e) {
                throw new WorkflowExecutionException("Received interruption signal while waiting for workflow to terminate");
            }
            // TODO review: actual log operations needed here? also unsubscribe from nodes! - misc_ro
            log.debug("Shutting down log files");
        } catch (WorkflowExecutionException e) {
            headlessWfExeCtx.closeResources();
            outputReceiver.addOutput("Failed to execute workflow '" + wfFile.getAbsolutePath() + "': " + e.getMessage());
            throw e;
        }
        headlessWfExeCtx.closeResources();
        headlessWfExeCtx.addOutput("Terminated execution of workflow '" + wfFile.getAbsolutePath() + "'. Final state: "
            + finalState.getDisplayName());

        // map to reduced set of final workflow states (to avoid downstream checking for invalid values)
        switch (finalState) {
        case FINISHED:
            return FinalWorkflowState.FINISHED;
        case CANCELLED:
            return FinalWorkflowState.CANCELLED;
        case FAILED:
            return FinalWorkflowState.FAILED;
        default:
            throw new WorkflowExecutionException("Unexpected value for final workflow state: " + finalState.getDisplayName());
        }
    }

    private void startWorkflowExecution(final WorkflowDescription wd, final HeadlessWorkflowExecutionContext wfHeadlessExeCtx)
        throws WorkflowExecutionException {
        File logDirectory = wfHeadlessExeCtx.getLogDirectory();
        logDirectory.mkdirs();
        if (!logDirectory.isDirectory()) {
            throw new WorkflowExecutionException("Failed to create workflow log directory " + logDirectory.getAbsolutePath());
        }
        log.debug("Writing detail workflow log files to " + logDirectory.getAbsolutePath());

        // instantiate the WorkflowLaunchConfigurationHelper
        if (localNode == null) {
            localNode = platformService.getLocalNodeId();
        }

        // Set null platforms to localPlatform
        DistributedComponentKnowledge compKnowledge = compKnowledgeService.getCurrentComponentKnowledge();

        for (WorkflowNode node : wd.getWorkflowNodes()) {
            // replace null (representing localhost) with the actual host name
            if (node.getComponentDescription().getNode() == null) {
                Collection<ComponentInstallation> installations = compKnowledge.getLocalInstallations();
                ComponentInstallation installation = ComponentUtils.getExactMatchingComponentInstallationForNode(
                    node.getComponentDescription().getIdentifier(), installations, localNode);
                if (installation == null) {
                    throw new WorkflowExecutionException(String.format("Component '%s' not installed on node %s "
                        + node.getName(), node.getComponentDescription().getNode().getAssociatedDisplayName()));
                }
                node.getComponentDescription().setComponentInstallationAndUpdateConfiguration(installation);
            }
        }

        if (wd.getControllerNode() == null) {
            wd.setControllerNode(localNode);
        }

        wd.setName(WorkflowExecutionUtils.generateDefaultNameforExecutingWorkflow(wfHeadlessExeCtx.getWorkflowFile().getName(), wd));

        if (!WorkflowDescriptionValidator.isWorkflowDescriptionValid(wd, wfHeadlessExeCtx.getUser())) {
            throw new WorkflowExecutionException("Workflow description invalid: " + wfHeadlessExeCtx.getWorkflowFile().getAbsolutePath());
        }

        WorkflowExecutionContextBuilder wfExeCtxBuilder = new WorkflowExecutionContextBuilder(wd);
        wfExeCtxBuilder.setInstanceName(wd.getName());
        wfExeCtxBuilder.setNodeIdentifierStartedExecution(localNode);
        final de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext wfExeCtx = wfExeCtxBuilder.build();
        wfHeadlessExeCtx.setWorkflowExecutionContext(wfExeCtx);

        WorkflowStateNotificationSubscriber workflowStateChangeListener =
            new WorkflowStateNotificationSubscriber(new WorkflowStateChangeListener() {

                @Override
                public void onNewWorkflowState(String workflowIdentifier, WorkflowState newState) {
                    log.debug("Received state change event for workflow " + workflowIdentifier + ": " + newState.getDisplayName());
                    switch (newState) {
                    case CANCELLED:
                    case FAILED:
                    case FINISHED:
                        boolean getDisposed = wfHeadlessExeCtx.getDispose() == Dispose.Always
                            || (newState == WorkflowState.FINISHED && wfHeadlessExeCtx.getDispose() == Dispose.OnFinished);
                        wfHeadlessExeCtx.reportWorkflowTerminated(newState, getDisposed);
                        if (getDisposed) {
                            try {
                                workflowExecutionService.dispose(workflowIdentifier, wfHeadlessExeCtx
                                    .getWorkflowExecutionContext().getNodeId());
                            } catch (CommunicationException | RuntimeException e) {
                                log.error("Failed to dispose workflow", e);
                                wfHeadlessExeCtx.reportWorkflowDisposed(WorkflowState.FAILED);
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
            });
        
        // add workflow state subscriber; only subscribe for this specific workflow (no wildcard)
        try {
            distributedNotificationService.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID
                + wfExeCtx.getExecutionIdentifier(), workflowStateChangeListener,
                wfExeCtx.getNodeId());
        } catch (CommunicationException e1) {
            log.error("Failed to start workflow (error while subscribing for state changes): " + e1.getMessage());
            return;
        }

        // add console output subscriber
        ConsoleRowSubscriber consoleRowSubscriber = new ConsoleRowSubscriber(wfHeadlessExeCtx, logDirectory);
        wfHeadlessExeCtx.registerResourceToCloseOnFinish(consoleRowSubscriber);
        
        // subscribe to a console row notification on workflow controller node
        try {
            distributedNotificationService.subscribe(String.format("%s%s" + ConsoleRow.NOTIFICATION_SUFFIX,
                wfExeCtx.getExecutionIdentifier(), wfExeCtx.getNodeId().getIdString()),
                consoleRowSubscriber, wfExeCtx.getNodeId());
        } catch (CommunicationException e1) {
            log.error("Failed to start workflow (error while subscribing for console row output): " + e1.getMessage());
            return;
        }
        
        // needed to guarantee this NotificationSubscriber is not removed by GC and thus, can be accessible from remote. get obsolete if
        // following issue is resolved: https://www.sistec.dlr.de/mantis/view.php?id=8659 -- Jan 2015 seid_do
        wfHeadlessExeCtx.setConsoleRowSubscriber(consoleRowSubscriber);
        wfHeadlessExeCtx.setWorkflowStateChangeListener(workflowStateChangeListener);
        
        // Start the workflow
        WorkflowExecutionInformation wfExeInfo;
        try {
            wfExeInfo = workflowExecutionService.execute(wfExeCtx);
        } catch (CommunicationException e) {
            throw new WorkflowExecutionException(e);
        }
        
        log.debug(String.format("Created workflow from file %s with name '%s', with id %s on node %s (%s)",
            wfHeadlessExeCtx.getWorkflowFile().getName(), wfExeInfo.getInstanceName(), wfExeInfo.getExecutionIdentifier(),
            wfExeInfo.getNodeId().getAssociatedDisplayName(), wfExeInfo.getNodeId().getIdString()));
    }

    private WorkflowDescription loadWorkflowDescription(HeadlessWorkflowExecutionContext context) throws WorkflowExecutionException {
        File file = context.getWorkflowFile();
        User user = context.getUser();
        try {
            InputStream inputStream = new FileInputStream(file);
            String descriptionString = IOUtils.toString(inputStream, WorkflowConstants.ENCODING_UTF8);
            inputStream.close();
            PersistentWorkflowDescription description = persistentWorkflowDescriptionUpdateService.createPersistentWorkflowDescription(
                descriptionString, user);
            boolean hasSilentUpdate = isUpdateForWorkflowAvailable(description, user, true);
            boolean hasNonSilentUpdate = isUpdateForWorkflowAvailable(description, user, false);
            if (description != null) {
                if (hasSilentUpdate || hasNonSilentUpdate) {
                    if (hasNonSilentUpdate) {
                        createWorkflowFileBackup(file, context);
                    }
                    InputStream is = performUpdateForWorkflow(description, user);
                    description = persistentWorkflowDescriptionUpdateService.createPersistentWorkflowDescription(
                        IOUtils.toString(is, WorkflowConstants.ENCODING_UTF8), user);
                    is.close();
                    FileUtils.writeStringToFile(context.getWorkflowFile(), description.getWorkflowDescriptionAsString());
                }
                InputStream descStream = IOUtils.toInputStream(description.getWorkflowDescriptionAsString());
                WorkflowDescription updatedDescription =
                    new WorkflowDescriptionPersistenceHandler().readWorkflowDescriptionFromStream(descStream, user);
                descStream.close();
                log.debug("Created workflow description " + updatedDescription.getIdentifier() + " from workflow file "
                    + file.getAbsolutePath());
                return updatedDescription;
            } else {
                throw new WorkflowExecutionException("Failed to load workflow description: Description is null or failed to parse.");
            }
        } catch (IOException | ParseException | RuntimeException e) {
            log.warn("Failed to load workflow description", e);
            throw new WorkflowExecutionException("Failed to load workflow description: " + e.toString());
        }
    }

    private void applyPlaceholdersAndVerify(final WorkflowDescription workflowDescription,
        final File placeholdersFile) throws WorkflowExecutionException {

        final Map<String, Map<String, String>> placeholderValues;
        if (placeholdersFile != null) {
            placeholderValues = readPlaceholdersFile(placeholdersFile);
            log.debug(String.format("Loaded placeholder values from %s: %s", placeholdersFile.getAbsolutePath(), placeholderValues));
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
                    missingPlaceholderValues.add(String.format("\"%s\" -> \"%s\" (%s)", componentId, missingKey, compInstanceId));
                }
            }

            // apply available values
            if (ciPlaceholderValues != null && !ciPlaceholderValues.isEmpty()) {
                logPlaceholderValues(wn, ciPlaceholderValues);
                wn.getComponentDescription().getConfigurationDescription().setPlaceholders(ciPlaceholderValues);
            }
        }

        // check if missing set contains entries -> fail if it does
        if (!missingPlaceholderValues.isEmpty()) {
            throw new WorkflowExecutionException("The workflow requires additional placeholder values "
                + "(listed as <component id>/<version> -> <placeholder key> (<instance id>)): " + missingPlaceholderValues);
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
        log.debug(String.format("Applying %d placeholder value(s) to workflow node %s: %s", cPlaceholderValues.size(), wn,
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

    private Map<String, Map<String, String>> readPlaceholdersFile(File placeholdersFile) throws WorkflowExecutionException {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(placeholdersFile,
                new TypeReference<HashMap<String, HashMap<String, String>>>() {
                });
        } catch (IOException e) {
            throw new WorkflowExecutionException("Failed to parse placeholders file " + placeholdersFile.getAbsolutePath() + ": "
                + e.toString());
        }
    }

    private void createWorkflowFileBackup(File file, HeadlessWorkflowExecutionContext context) {
        String backupFilename = PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(file.getName());
        File backup = new File(file.getParentFile().getAbsolutePath() + File.separator + backupFilename + ".wf");
        try {
            FileUtils.copyFile(file, backup);
            context.addOutput("Given workflow file was outdated. "
                + "It was updated before workflow execution. A backup file was created: " + backupFilename);
        } catch (IOException e) {
            context.addOutput("Creating backup failed (" + backupFilename + "): " + e.getMessage());
        }
    }

    private void verifyIsReadableFile(File file, String type) throws WorkflowExecutionException {
        if (!file.isFile()) {
            throw new WorkflowExecutionException("The " + type + " file " + file.getAbsolutePath()
                + " does not exist or is not a normal file");
        }
        if (!file.canRead()) {
            throw new WorkflowExecutionException("The " + type + " file " + file.getAbsolutePath()
                + " exists, but cannot be read (maybe due to lack of file system permissions)");
        }
    }

    /**
     * Checks if for a given {@link WorkflowDescription} updates are available.
     * 
     * @param description to check for updates
     * @param user acting user
     * @param silentUpdate <code>true</code> it checks if silent update is available, otherwise
     *        <code>false</code>
     * @return <code>true</code> if update is available, otherwise <code>false</code>
     * @throws IOException on error
     */
    private boolean isUpdateForWorkflowAvailable(PersistentWorkflowDescription description, User user, boolean silentUpdate)
        throws IOException {
        return persistentWorkflowDescriptionUpdateService.isUpdateForWorkflowDescriptionAvailable(description, silentUpdate);
    }

    /**
     * Checks if for a given {@link WorkflowDescription} updates are available.
     * 
     * @param description wf file (as input stream)
     * @param user acting user
     * @return <code>true</code> if update is available, otherwise <code>false</code>
     * @throws IOException on error
     */
    private InputStream performUpdateForWorkflow(PersistentWorkflowDescription description, User user) throws IOException {
        PersistentWorkflowDescription workflowDescription = persistentWorkflowDescriptionUpdateService
            .performWorkflowDescriptionUpdate(description);
        return IOUtils.toInputStream(workflowDescription.getWorkflowDescriptionAsString());

    }

    /**
     * Bind method. Set to public for test purposes.
     * 
     * @param newInstance new service instance.
     */
    public void bindDistributedNotificationService(DistributedNotificationService newInstance) {
        distributedNotificationService = newInstance;
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        compKnowledgeService = newInstance;
    }
    
    protected void bindWorkflowHostService(WorkflowHostService newInstance) {
        workflowHostService = newInstance;
    }

    /**
     * Bind method. Set to public for test purposes.
     * 
     * @param newService new service instance.
     */
    public void bindWorkflowExecutionService(WorkflowExecutionService newService) {
        workflowExecutionService = newService;
    }

    /**
     * Bind method. Set to public for test purposes.
     * 
     * @param newInstance new service instance.
     */
    public void bindWorkflowDescriptionUpdateService(PersistentWorkflowDescriptionUpdateService
        newInstance) {
        persistentWorkflowDescriptionUpdateService = newInstance;
    }

    /**
     * Bind method. Set to public for test purposes.
     * 
     * @param newInstance new service instance.
     */
    public void bindPlatformService(PlatformService newInstance) {
        platformService = newInstance;
    }

    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations() {
        return workflowExecutionService.getWorkflowExecutionInformations();
    }
    
    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations(boolean forceRefresh) {
        return workflowExecutionService.getWorkflowExecutionInformations(forceRefresh);
    }


    @Override
    public void cancel(String executionId, NodeIdentifier node) throws CommunicationException {
        workflowExecutionService.cancel(executionId, node);
    }

    @Override
    public void pause(String executionId, NodeIdentifier node) throws CommunicationException {
        workflowExecutionService.pause(executionId, node);
    }

    @Override
    public void resume(String executionId, NodeIdentifier node) throws CommunicationException {
        workflowExecutionService.resume(executionId, node);
    }

    @Override
    public void dispose(String executionId, NodeIdentifier node) throws CommunicationException {
        workflowExecutionService.dispose(executionId, node);

    }

    @Override
    public WorkflowState getWorkflowState(String executionId, NodeIdentifier node) throws CommunicationException {
        return workflowExecutionService.getWorkflowState(executionId, node);
    }


}

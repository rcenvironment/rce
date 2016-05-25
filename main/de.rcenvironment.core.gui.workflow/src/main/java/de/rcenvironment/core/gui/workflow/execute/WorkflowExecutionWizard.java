/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowModelService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage.Type;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidatorUtils;
import de.rcenvironment.core.gui.workflow.view.OpenReadOnlyWorkflowRunEditorAction;
import de.rcenvironment.core.gui.workflow.view.properties.InputModel;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * {@link Wizard} to start the execution of a workflow.
 * 
 * @author Christian Weiss
 * @author Doreen Seider
 * @author Goekhan Guerkan
 */
public class WorkflowExecutionWizard extends Wizard implements DistributedComponentKnowledgeListener {

    private static final int MINIMUM_HEIGHT = 250;

    private static final int MINIMUM_WIDTH = 500;

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionWizard.class);

    private static final String BULLET_POINT = "- ";
    
    private final boolean inputTabEnabled;

    private final IFile wfFile;

    private WorkflowDescription wfDescription;

    private List<WorkflowNode> disabledWorkflowNodes;

    private List<Connection> disabledConnections;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private final WorkflowExecutionService workflowExecutionService;

    private final NodeIdentifierConfigurationHelper nodeIdConfigHelper;

    private WorkflowPage workflowPage;

    private PlaceholderPage placeholdersPage;

    private NodeIdentifier localNodeId;

    private String errorMessage;

    private String errorComponents;

    private boolean errorVisible = false;

    public WorkflowExecutionWizard(final IFile workflowFile, WorkflowDescription workflowDescription) {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        workflowExecutionService = serviceRegistryAccess.getService(WorkflowExecutionService.class);
        Activator.getInstance().registerUndisposedWorkflowShutdownListener();

        this.inputTabEnabled = serviceRegistryAccess.getService(ConfigurationService.class).getConfigurationSegment("general")
            .getBoolean(ComponentConstants.CONFIG_KEY_ENABLE_INPUT_TAB, false);
        
        this.wfFile = workflowFile;

        this.disabledWorkflowNodes = WorkflowExecutionUtils.getDisabledWorkflowNodes(workflowDescription);
        this.disabledConnections = workflowDescription.removeWorkflowNodesAndRelatedConnections(disabledWorkflowNodes);
        this.wfDescription = workflowDescription;

        nodeIdConfigHelper = new NodeIdentifierConfigurationHelper();
        // cache the local instance for later use
        this.localNodeId = serviceRegistryAccess.getService(PlatformService.class).getLocalNodeId();

        wfDescription.setName(WorkflowExecutionUtils.generateDefaultNameforExecutingWorkflow(workflowFile.getName(), wfDescription));
        wfDescription.setFileName(workflowFile.getName());

        // set the title of the wizard dialog
        setWindowTitle(Messages.workflowExecutionWizardTitle);
        // display a progress monitor
        setNeedsProgressMonitor(true);

        ColorPalette.getInstance().loadColors();
        serviceRegistryAccess.registerService(DistributedComponentKnowledgeListener.class, this);

    }

    @Override
    public void addPages() {
        workflowPage = new WorkflowPage(wfDescription.clone(), nodeIdConfigHelper);
        addPage(workflowPage);
        placeholdersPage = new PlaceholderPage(wfDescription.clone());
        addPage(placeholdersPage);
        getShell().setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);

    }

    @Override
    public boolean canFinish() {

        return workflowPage.canFinish()
            && (getContainer().getCurrentPage() == placeholdersPage || placeholdersPage.canFinish());
    }

    @Override
    public boolean performFinish() {

        grabDataFromWorkflowPage();

        if (!performValidations()) {
            return false;
        }

        if (!validateWorkflowAndPlaceholders() && !requestConfirmationForValidationErrorsWarnings()) {
            // keeps the execute dialog open
            return false;
        }

        WorkflowExecutionUtils.setNodeIdentifiersToTransientInCaseOfLocalOnes(wfDescription, localNodeId);
        saveWorkflow();

        grabDataFromPlaceholdersPage();

        Job job = new Job(Messages.workflowExecutionWizardTitle) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.settingUpWorkflow, 2);
                    monitor.worked(1);
                    executeWorkflowInBackground();
                    monitor.worked(1);
                    return Status.OK_STATUS;
                } finally {
                    monitor.done();
                }
            };
        };
        job.setUser(true);
        job.schedule();

        return true;
    }

    /**
     * @return true if all selected instances available.
     */
    public synchronized boolean performValidations() {
        WorkflowDescriptionValidationResult validationResult = workflowExecutionService.validateWorkflowDescription(wfDescription);
        workflowPage.getWorkflowComposite().refreshContent();

        if (getContainer().getCurrentPage() == placeholdersPage) {

            if (!validationResult.isSucceeded()) {
                // MessageDialog.openError(getShell(), "Instances Error", "Some instances selected, are not available anymore:\n\n"
                // + validationResult.toString() + "\n\nCheck your connection(s) or select (an)other instance(s).");
                if (!errorVisible) {
                    errorVisible = true;
                    MessageBox errorBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
                    errorBox.setMessage("Some instances selected, are not available anymore:\n\n"
                        + validationResult.toString() + "\n\nCheck your connection(s) or select (an)other instance(s).");
                    errorBox.setText("Instances Error");
                    int id = errorBox.open();
                    if (id == SWT.OK || id == SWT.CLOSE) {
                        errorVisible = false;
                    }
                }
                return false;
            }

        }

        if (validationResult.isSucceeded()) {
            return true;
        }

        return false;

    }

    private void grabDataFromWorkflowPage() {
        wfDescription.setName(workflowPage.getWorkflowName());
        wfDescription.setControllerNode(workflowPage.getControllerNodeId());
        wfDescription.setAdditionalInformation(workflowPage.getAdditionalInformation());
        Map<String, ComponentInstallation> cmpInstallations = workflowPage.getComponentInstallations();
        for (String wfNodeId : cmpInstallations.keySet()) {
            wfDescription.getWorkflowNode(wfNodeId).getComponentDescription()
                .setComponentInstallationAndUpdateConfiguration(cmpInstallations.get(wfNodeId));
        }
    }

    private void grabDataFromPlaceholdersPage() {
        placeholdersPage.performFinish();
        Map<String, Map<String, String>> placeholders = placeholdersPage.getPlaceholders();
        for (String wfNodeId : placeholders.keySet()) {
            wfDescription.getWorkflowNode(wfNodeId).getComponentDescription()
                .getConfigurationDescription().setPlaceholders(placeholders.get(wfNodeId));
        }
    }

    /**
     * Validate the executed workflow and the placeholder page, if any warnings or errors exist.
     * 
     * @return <code>false</code>, if at least one error or one warning exist
     */
    private boolean validateWorkflowAndPlaceholders() {
        Map<String, String> componentNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        WorkflowNodeValidatorUtils.initializeMessages(wfDescription);

        // workflow-error
        int workflowErrorAmount = WorkflowNodeValidatorUtils.getWorkflowErrors();
        boolean workflowError = WorkflowNodeValidatorUtils.hasErrors();
        componentNames.putAll(WorkflowNodeValidatorUtils.getComponentNames(Type.ERROR));

        // workflow-warning
        int workflowWarningAmount = WorkflowNodeValidatorUtils.getWorkflowWarnings();
        boolean workflowWarning = WorkflowNodeValidatorUtils.hasWarnings();
        componentNames.putAll(WorkflowNodeValidatorUtils.getComponentNames(Type.WARNING));

        // placeholder-error
        boolean placeholderError = placeholdersPage.validateErrors();
        int placeholderErrorAmount = placeholdersPage.getErrorAmount();
        componentNames.putAll(placeholdersPage.getComponentNamesWithError(true));

        createErrorMessage(placeholderError, workflowError, workflowWarning, placeholderErrorAmount + workflowErrorAmount,
            workflowWarningAmount, componentNames);

        return !(placeholderError || workflowError || workflowWarning);
    }

    private void createErrorMessage(boolean placeholderError, boolean workflowError, boolean workflowWarning,
        int errorAmount, int warningAmount, Map<String, String> componentNames) {
        errorMessage = "";
        errorComponents = "";
        List<String> messages = new ArrayList<String>();
        if (placeholderError || workflowError) {
            messages.add(StringUtils.format(Messages.errorMessage, errorAmount));
        }
        if (workflowWarning) {
            messages.add(StringUtils.format(Messages.workflowWarningMessage, warningAmount));
        }

        for (String message : messages) {
            if (errorMessage.equals("")) {
                errorMessage = message;
            } else {
                errorMessage += "and " + message;
            }
        }

        for (Entry<String, String> entry : componentNames.entrySet()) {
            if (errorComponents.equals("")) {
                errorComponents = BULLET_POINT + entry.getKey() + entry.getValue();
            } else {
                errorComponents += "\n" + BULLET_POINT + entry.getKey() + entry.getValue();
            }
        }
    }

    /**
     * @return true if proceed is clicked, false if cancel is clicked
     */
    private boolean requestConfirmationForValidationErrorsWarnings() {
        String[] dialogButtons = { Messages.proceedButton, Messages.cancelButton };
        MessageDialog confirmationDialog = new MessageDialog(getShell(), Messages.validationTitle, null,
            StringUtils.format(Messages.validationMessage, errorMessage, errorComponents), MessageDialog.QUESTION, dialogButtons, 1);
        return confirmationDialog.open() == 0;
    }

    private void saveWorkflow() {
        wfDescription.addWorkflowNodesAndConnections(disabledWorkflowNodes, disabledConnections);
        WorkflowDescriptionPersistenceHandler persistenceHandler = new WorkflowDescriptionPersistenceHandler();
        try (ByteArrayOutputStream content = persistenceHandler.writeWorkflowDescriptionToStream(wfDescription)) {
            ByteArrayInputStream input = new ByteArrayInputStream(content.toByteArray());
            wfFile.setContents(input, // the file content
                true, // keep saving, even if IFile is out of sync with the Workspace
                false, // dont keep history
                null); // progress monitor
            wfFile.getProject().refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException | IOException e) {
            MessageDialog.openError(getShell(), "Error when Saving Workflow", "Failed to save workflow: " + e.getMessage());
            LOG.error(StringUtils.format("Failed to save workflow: %s", wfFile.getRawLocation().toOSString()));
        }

    }

    private void executeWorkflowInBackground() {

        DistributedComponentKnowledge compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class)
            .getCurrentComponentKnowledge();

        try {
            WorkflowExecutionUtils.replaceNullNodeIdentifiersWithActualNodeIdentifier(wfDescription, localNodeId, compKnowledge);
        } catch (WorkflowExecutionException e) {
            handleWorkflowExecutionError(e);
            return;
        }

        String name = wfDescription.getName();
        if (name == null) {
            name = Messages.bind(Messages.defaultWorkflowName, wfFile.getName().toString());
        }

        WorkflowExecutionContextBuilder wfExeCtxBuilder = new WorkflowExecutionContextBuilder(wfDescription);
        wfExeCtxBuilder.setInstanceName(name);
        wfExeCtxBuilder.setNodeIdentifierStartedExecution(localNodeId);
        if (wfDescription.getAdditionalInformation() != null && !wfDescription.getAdditionalInformation().isEmpty()) {
            wfExeCtxBuilder.setAdditionalInformationProvidedAtStart(wfDescription.getAdditionalInformation());
        }
        WorkflowExecutionContext wfExecutionContext = wfExeCtxBuilder.build();

        final WorkflowExecutionInformation wfExeInfo;
        try {
            wfExeInfo = workflowExecutionService.executeWorkflowAsync(wfExecutionContext);
        } catch (WorkflowExecutionException | RemoteOperationException e) {
            handleWorkflowExecutionError(e);
            return;
        }

        // before starting the workflow, ensure that the console model is initialized
        // so that no console output gets lost; this is lazily initialized here
        // so the application startup is not slowed down
        try {
            serviceRegistryAccess.getService(ConsoleRowModelService.class).ensureConsoleCaptureIsInitialized();
        } catch (InterruptedException e) {
            LOG.error("Failed initialize workflow console capturing for workflow: " + wfDescription.getName(), e);
        }

        if (inputTabEnabled) {
            InputModel.ensureInputCaptureIsInitialized();
        }

        WorkflowExecutionUtils.removeDisabledWorkflowNodesWithoutNotify(wfExeInfo.getWorkflowDescription());
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                new OpenReadOnlyWorkflowRunEditorAction(wfExeInfo).run();
            }
        });

    }

    private void handleWorkflowExecutionError(final Throwable e) {
        LOG.error("Failed to execute workflow: " + wfDescription.getName(), e);
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                MessageDialog.openError(getShell(), "Workflow Execution Error", "Failed to execute workflow: " + e.getMessage());
            }
        });
    }

    @Override
    public void dispose() {
        serviceRegistryAccess.dispose();
        super.dispose();
    }

    @Override
    public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {

        if (PlatformUI.isWorkbenchRunning() && !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (workflowPage != null && !workflowPage.getControl().isDisposed()) {
                        performValidations();
                    } else {
                        LogFactory.getLog(getClass()).warn("Got callback (onDistributedComponentKnowledgeChanged)"
                            + " but widget(s) already disposed; the listener might not be disposed properly");
                    }
                }
            });

        }
    }
}

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
import java.util.List;
import java.util.Map;

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

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierContextHolder;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessageStore;
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
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowDescriptionValidationUtils;
import de.rcenvironment.core.gui.workflow.view.WorkflowRunEditorAction;
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
 * @author Jascha Riedel
 * @author Robert Mischke
 */
public class WorkflowExecutionWizard extends Wizard implements DistributedComponentKnowledgeListener {

    private static final int MINIMUM_HEIGHT = 250;

    private static final int MINIMUM_WIDTH = 500;

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionWizard.class);

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

    private LogicalNodeId localDefaultNodeId;

    private boolean errorVisible = false;

    private ComponentValidationMessageStore messageStore = ComponentValidationMessageStore.getInstance();

    public WorkflowExecutionWizard(final IFile workflowFile, WorkflowDescription workflowDescription) {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        workflowExecutionService = serviceRegistryAccess.getService(WorkflowExecutionService.class);
        Activator.getInstance().registerUndisposedWorkflowShutdownListener();

        this.inputTabEnabled = serviceRegistryAccess.getService(ConfigurationService.class)
            .getConfigurationSegment("general").getBoolean(ComponentConstants.CONFIG_KEY_ENABLE_INPUT_TAB, false);

        this.wfFile = workflowFile;

        this.disabledWorkflowNodes = WorkflowExecutionUtils.getDisabledWorkflowNodes(workflowDescription);
        this.disabledConnections = workflowDescription.removeWorkflowNodesAndRelatedConnections(disabledWorkflowNodes);
        this.wfDescription = workflowDescription;

        nodeIdConfigHelper = new NodeIdentifierConfigurationHelper();
        // cache the local instance for later use
        this.localDefaultNodeId = serviceRegistryAccess.getService(PlatformService.class).getLocalDefaultLogicalNodeId();

        wfDescription.setName(
            WorkflowExecutionUtils.generateDefaultNameforExecutingWorkflow(workflowFile.getName(), wfDescription));
        wfDescription.setFileName(workflowFile.getName());

        // set the title of the wizard dialog
        setWindowTitle(Messages.workflowExecutionWizardTitle);
        // display a progress monitor
        setNeedsProgressMonitor(true);

        ColorPalette.getInstance().loadColors();
        serviceRegistryAccess.registerService(DistributedComponentKnowledgeListener.class, this);

        // this is currently required for nested calls to WorkflowDescription.clone(), which indirectly use id deserialization - misc_ro
        NodeIdentifierContextHolder
            .setDeserializationServiceForCurrentThread(serviceRegistryAccess.getService(NodeIdentifierService.class));
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

        grabDataFromPlaceholdersPage();

        if (!performValidations()) {
            return false;
        }

        if (!validateWorkflowAndPlaceholders() && !requestConfirmationForValidationErrorsWarnings()) {
            return false;
        } else {
            //Only save placeholders to persistent settings if the user does not cancel the run dialog.
            placeholdersPage.savePlaceholdersToPersistentSettings();
        }

        placeholdersPage.dispose();

        WorkflowExecutionUtils.setNodeIdentifiersToTransientInCaseOfLocalOnes(wfDescription, localDefaultNodeId);
        saveWorkflow();

        // clone the wf description here to make sure that the description instance passed to the workflow engine is not modified by the
        // GUI, can and should be changed with #0012071 e.g., by introducing and using an immutable representation of the workflow
        executeWorkflowInBackground(wfDescription.clone());

        return true;
    }

    private void executeWorkflowInBackground(final WorkflowDescription clonedWfDescription) {
        Job job = new Job(Messages.workflowExecutionWizardTitle) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.settingUpWorkflow, 2);
                    monitor.worked(1);
                    executeWorkflow(clonedWfDescription);
                    monitor.worked(1);
                    return Status.OK_STATUS;
                } finally {
                    monitor.done();
                }
            };
        };
        job.setUser(true);
        job.schedule();
    }

    /**
     * @return true if all selected instances available.
     */
    public synchronized boolean performValidations() {
        WorkflowDescriptionValidationResult validationResult = workflowExecutionService
            .validateWorkflowDescription(wfDescription);
        workflowPage.getWorkflowComposite().refreshContent();

        if (getContainer().getCurrentPage() == placeholdersPage) {

            if (!validationResult.isSucceeded()) {
                // MessageDialog.openError(getShell(), "Instances Error", "Some
                // instances selected, are not available anymore:\n\n"
                // + validationResult.toString() + "\n\nCheck your connection(s)
                // or select (an)other instance(s).");
                if (!errorVisible) {
                    errorVisible = true;
                    MessageBox errorBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
                    errorBox.setMessage("Some of the selected instances are not available anymore:\n\n"
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
            wfDescription.getWorkflowNode(wfNodeId).getComponentDescription().getConfigurationDescription()
                .setPlaceholders(placeholders.get(wfNodeId));
        }
    }

    /**
     * Validate the executed workflow and the placeholder page, if any warnings or errors exist.
     * 
     * @return <code>false</code>, if at least one error or one warning exist
     */
    private boolean validateWorkflowAndPlaceholders() {
        WorkflowDescriptionValidationUtils.validateWorkflowDescription(wfDescription, true, true);

        // placeholder-error
        boolean placeholderError = placeholdersPage.validateErrors();

        boolean returnValue = !placeholderError && messageStore.isErrorAndWarningsFree();
        return returnValue;
    }

    /**
     * @return true if proceed is clicked, false if cancel is clicked
     */
    private boolean requestConfirmationForValidationErrorsWarnings() {

        WorkflowExecutionWizardValidationDialog dialog = new WorkflowExecutionWizardValidationDialog(getShell(),
            messageStore.getMessageMap(), wfDescription, placeholdersPage);
        return dialog.open() == 0;
    }

    private void saveWorkflow() {
        wfDescription.addWorkflowNodesAndConnections(disabledWorkflowNodes, disabledConnections);
        WorkflowDescriptionPersistenceHandler persistenceHandler = new WorkflowDescriptionPersistenceHandler();
        try (ByteArrayOutputStream content = persistenceHandler.writeWorkflowDescriptionToStream(wfDescription)) {
            ByteArrayInputStream input = new ByteArrayInputStream(content.toByteArray());
            wfFile.setContents(input, // the file content
                true, // keep saving, even if IFile is out of sync with the
                      // Workspace
                false, // dont keep history
                null); // progress monitor
            wfFile.getProject().refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException | IOException e) {
            MessageDialog.openError(getShell(), "Error when Saving Workflow",
                "Failed to save workflow: " + e.getMessage());
            LOG.error(StringUtils.format("Failed to save workflow: %s", wfFile.getRawLocation().toOSString()));
        }

    }

    private void executeWorkflow(WorkflowDescription clonedWfDesc) {

        DistributedComponentKnowledge compKnowledge = serviceRegistryAccess
            .getService(DistributedComponentKnowledgeService.class).getCurrentComponentKnowledge();

        try {
            WorkflowExecutionUtils.replaceNullNodeIdentifiersWithActualNodeIdentifier(clonedWfDesc, localDefaultNodeId, compKnowledge);
        } catch (WorkflowExecutionException e) {
            handleWorkflowExecutionError(clonedWfDesc, e);
            return;
        }

        String name = clonedWfDesc.getName();
        if (name == null) {
            name = Messages.bind(Messages.defaultWorkflowName, wfFile.getName().toString());
        }

        WorkflowExecutionContextBuilder wfExeCtxBuilder = new WorkflowExecutionContextBuilder(clonedWfDesc);
        wfExeCtxBuilder.setInstanceName(name);
        wfExeCtxBuilder.setNodeIdentifierStartedExecution(localDefaultNodeId);
        if (clonedWfDesc.getAdditionalInformation() != null && !clonedWfDesc.getAdditionalInformation().isEmpty()) {
            wfExeCtxBuilder.setAdditionalInformationProvidedAtStart(clonedWfDesc.getAdditionalInformation());
        }
        WorkflowExecutionContext wfExecutionContext = wfExeCtxBuilder.build();

        final WorkflowExecutionInformation wfExeInfo;
        try {
            wfExeInfo = workflowExecutionService.executeWorkflowAsync(wfExecutionContext);
        } catch (WorkflowExecutionException | RemoteOperationException e) {
            handleWorkflowExecutionError(clonedWfDesc, e);
            return;
        }

        // before starting the workflow, ensure that the console model is
        // initialized
        // so that no console output gets lost; this is lazily initialized here
        // so the application startup is not slowed down
        try {
            serviceRegistryAccess.getService(ConsoleRowModelService.class).ensureConsoleCaptureIsInitialized();
        } catch (InterruptedException e) {
            LOG.error("Failed initialize workflow console capturing for workflow: " + clonedWfDesc.getName(), e);
        }

        if (inputTabEnabled) {
            InputModel.ensureInputCaptureIsInitialized();
        }

        WorkflowExecutionUtils.removeDisabledWorkflowNodesWithoutNotify(wfExeInfo.getWorkflowDescription());
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                new WorkflowRunEditorAction(wfExeInfo).run();
            }
        });

    }

    private void handleWorkflowExecutionError(WorkflowDescription clonedWfDesc, final Throwable e) {
        LOG.error("Failed to execute workflow: " + clonedWfDesc.getName(), e);
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                MessageDialog.openError(getShell(), "Workflow Execution Error",
                    "Failed to execute workflow: " + e.getMessage());
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

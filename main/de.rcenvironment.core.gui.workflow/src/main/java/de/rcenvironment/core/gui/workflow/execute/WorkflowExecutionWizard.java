/*
 * Copyright (C) 2006-2014 DLR, Germany
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
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.communication.api.SimpleCommunicationService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowModelService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidator;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.view.OpenReadOnlyWorkflowRunEditorAction;
import de.rcenvironment.core.gui.workflow.view.properties.InputModel;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * {@link Wizard} to start the execution of a {@link WorkflowController}.
 * 
 * @author Christian Weiss
 */
public class WorkflowExecutionWizard extends Wizard {

    private static final String LOADING_WORKFLOW_FILE_FAILED = "loading workflow file failed";

    private static final Log LOGGER = LogFactory.getLog(WorkflowExecutionWizard.class);

    private static final String DE_RCENVIRONMENT_RCE_GUI_WORKFLOW = "de.rcenvironment.rce.gui.workflow";

    private final IFile workflowFile;

    private WorkflowDescription sourceWorkflowDescription;

    private boolean fromFile;
    
    private final WorkflowDescription workflowDescriptionforWorkflowPage;

    private final WorkflowExecutionConfigurationHelper executionHelper;

    private WorkflowDescription backingWorkflowDescription;

    private WorkflowPage workflowPage;

    private PlaceholderPage placeholderPage;

    private NodeIdentifier localNode;

    private final ServiceRegistryAccess serviceRegistryAccess;
    
    public WorkflowExecutionWizard(final IFile workflowFile, WorkflowDescription workflowDescription, boolean fromFile) {
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);

        Activator.getInstance().registerUndisposedWorkflowShutdownListener();

        this.workflowFile = workflowFile;
        this.sourceWorkflowDescription = workflowDescription;
        this.fromFile = fromFile;
        
        // retrieve the services needed for the WorkflowLaunchConfigurationHelper
        final SimpleCommunicationService scs = new SimpleCommunicationService();
        // instantiate the WorkflowLaunchConfigurationHelper
        executionHelper = new WorkflowExecutionConfigurationHelper(scs);
        // cache the local platform for later use
        this.localNode = scs.getLocalNodeId();
        // load the WorflowDescription from the provided IFile
        try {
            backingWorkflowDescription = sourceWorkflowDescription.clone();
        } catch (IllegalStateException e) {
            LOGGER.error(LOADING_WORKFLOW_FILE_FAILED, e);
            backingWorkflowDescription = null;
//        } catch (CoreException e) {
//            LOGGER.error(LOADING_WORKFLOW_FILE_FAILED, e);
//            backingWorkflowDescription = null;
//        } catch (IOException e) {
//            LOGGER.error(LOADING_WORKFLOW_FILE_FAILED, e);
//            backingWorkflowDescription = null;
//        } catch (ParseException e) {
//            LOGGER.error(LOADING_WORKFLOW_FILE_FAILED, e);
//            backingWorkflowDescription = null;
        }
        // cancel and display an error in case no workflow was selected
        if (backingWorkflowDescription == null) {
            final Status status = new Status(IStatus.ERROR, DE_RCENVIRONMENT_RCE_GUI_WORKFLOW, Messages.illegalExecutionSelectionMessage);
            ErrorDialog.openError(Display.getCurrent().getActiveShell(), Messages.illegalExecutionSelectionTitle,
                Messages.illegalExecutionSelectionMessage, status);
            throw new IllegalArgumentException(Messages.bind("File %s does not contain a valid workflow description.", workflowFile
                .getFullPath().toString()));
        }
        
        
        // clone the WorkflowDescription to operate on a copy instead of the real instance to avoid
        // unclean changes in case the user chooses to 'cancel' the process
        this.workflowDescriptionforWorkflowPage = removeInactiveWorkflowNodes(backingWorkflowDescription.clone());
        workflowDescriptionforWorkflowPage.setName(WorkflowExecutionUtils.generateDefaultNameforExecutingWorkflow(workflowFile.getName(),
            workflowDescriptionforWorkflowPage));

        // set the title of the wizard dialog
        setWindowTitle(Messages.workflowExecutionWizardTitle);
        // display a progress monitor
        setNeedsProgressMonitor(true);
    }

    private WorkflowDescription removeInactiveWorkflowNodes(WorkflowDescription workflowDescription) {
        List<WorkflowNode> nodes = new ArrayList<>();
        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            if (!node.isEnabled()) {
                nodes.add(node);
            }
        }
        workflowDescription.removeWorkflowNodesAndRelatedConnections(nodes);
        return workflowDescription;
    }
    
    protected WorkflowDescription getWorkflowDescription() {
        return workflowDescriptionforWorkflowPage;
    }

    protected WorkflowExecutionConfigurationHelper getHelper() {
        return executionHelper;
    }

    @Override
    public void addPages() {
        workflowPage = new WorkflowPage(this);
        addPage(workflowPage);
        placeholderPage = new PlaceholderPage(this);
        addPage(placeholderPage);
    }

    @Override
    public boolean canFinish() {
        // cannot complete the wizard from the first page
        if (workflowPage.getWorkflowComposite().areNodesValid()
            && (this.getContainer().getCurrentPage() == placeholderPage
            || placeholderPage.getComponentPlaceholderTree().getItemCount() == 0)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean performFinish() {
        placeholderPage.performFinish(backingWorkflowDescription); // do everything necessary with
                                                                   // placeholders
        Job job = new Job(Messages.workflowExecutionWizardTitle) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.settingUpWorkflow, 9);
                    IStatus status;
                    if (performWorkflowExection(monitor)) {
                        status = Status.OK_STATUS;
                    } else {
                        status = Status.CANCEL_STATUS;
                    }
                    monitor.worked(1);
                    return status;
                } finally {
                    monitor.done();
                }
            };
        };
        job.setUser(true);
        job.schedule();

        return true;
    }

    private boolean performWorkflowExection(IProgressMonitor monitor) {
        if (WorkflowDescriptionValidator.isWorkflowDescriptionValid(workflowDescriptionforWorkflowPage,
            Activator.getInstance().getUser())) {
            monitor.worked(4);
            backingWorkflowDescription.setName(workflowDescriptionforWorkflowPage.getName());
            backingWorkflowDescription.setControllerNode(workflowDescriptionforWorkflowPage.getControllerNode());

            for (WorkflowNode node : workflowDescriptionforWorkflowPage.getWorkflowNodes()) {
                WorkflowNode backingNode = backingWorkflowDescription.getWorkflowNode(node.getIdentifier());
                ComponentInstallation compInstallation = node.getComponentDescription().getComponentInstallation();
                backingNode.getComponentDescription().setComponentInstallationAndUpdateConfiguration(compInstallation);
            }
            monitor.worked(1);
            backingWorkflowDescription.setAdditionalInformation(workflowDescriptionforWorkflowPage.getAdditionalInformation());
        } else {
            final String reason = Messages.bind(Messages.illegalConfigReason,
                WorkflowDescriptionValidator.findUnreachableNode(workflowDescriptionforWorkflowPage,
                    Activator.getInstance().getUser()));
            final Status status = new Status(IStatus.ERROR, DE_RCENVIRONMENT_RCE_GUI_WORKFLOW, reason);
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    ErrorDialog.openError(getShell(), Messages.illegalConfigTitle, Messages.illegalConfigMessage, status);
                }
            });

            return false;
        }

        boolean executionSuccessful = false;
        try {
            // launch first and save only in case no exceptions occurred
            executeWorkflow();
            executionSuccessful = true;
            monitor.worked(2);
            saveWorkflow();
            monitor.worked(1);
        } catch (RuntimeException | InterruptedException | WorkflowExecutionException | CommunicationException e) {
            final String message;
            if (!executionSuccessful) {
                message = Messages.workflowLaunchFailed;
            } else {
                message = Messages.workflowSaveFailed;
            }
            LOGGER.error(message, e);
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message);
                }
            });

            return false;
        }
        return true;
    }

    private void saveWorkflow() {
        for (WorkflowNode wfNode : backingWorkflowDescription.getWorkflowNodes()) {
            ComponentDescription cd = wfNode.getComponentDescription();
            if (cd.getNode().equals(localNode)) {
                cd.setIsNodeTransient(true);
            }
        }
        WorkflowDescriptionPersistenceHandler persistenceHandler = new WorkflowDescriptionPersistenceHandler();
        try {
            ByteArrayOutputStream content = persistenceHandler.writeWorkflowDescriptionToStream(backingWorkflowDescription);
            ByteArrayInputStream input = new ByteArrayInputStream(content.toByteArray());
            workflowFile.setContents(input, // the file content
                true, // keep saving, even if IFile is out of sync with the Workspace
                false, // dont keep history
                null); // progress monitor
            
            // "merge" the workflow execution information back into the workflow description, which is the current editor input, if workflow
            // was executed from an open editor
            if (!fromFile) {
                sourceWorkflowDescription.copyExecutionInformationFromWorkflowDescription(backingWorkflowDescription);
            }
        } catch (CoreException e) {
            LOGGER.error(e.getStackTrace());
            throw new RuntimeException("Failed to persist workflow description", e);
        } catch (IOException e) {
            LOGGER.error(e.getStackTrace());
            throw new RuntimeException("Failed to persist workflow description", e);
        }
    }

    private void executeWorkflow() throws InterruptedException, WorkflowExecutionException, CommunicationException {

        DistributedComponentKnowledgeService service = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class);
        DistributedComponentKnowledge compKnowledge = service.getCurrentComponentKnowledge();

        WorkflowDescription runtimeWorkflowDescription = removeInactiveWorkflowNodes(backingWorkflowDescription.clone());
        for (WorkflowNode node : runtimeWorkflowDescription.getWorkflowNodes()) {
            // replace null (representing localhost) with the actual host name
            if (node.getComponentDescription().getNode() == null) {
                Collection<ComponentInstallation> installations = compKnowledge.getLocalInstallations();
                ComponentInstallation installation = ComponentUtils.getComponentInstallationForNode(
                    node.getComponentDescription().getIdentifier(), installations, localNode);
                node.getComponentDescription().setComponentInstallationAndUpdateConfiguration(installation);
            }
        }

        if (runtimeWorkflowDescription.getControllerNode() == null) {
            runtimeWorkflowDescription.setControllerNode(localNode);
        }

        String name = runtimeWorkflowDescription.getName();
        if (name == null) {
            name = Messages.bind(Messages.defaultWorkflowName, workflowFile.getName().toString());
        }
        
        WorkflowExecutionContextBuilder wfExeCtxBuilder = new WorkflowExecutionContextBuilder(runtimeWorkflowDescription);
        wfExeCtxBuilder.setInstanceName(name);
        wfExeCtxBuilder.setNodeIdentifierStartedExecution(localNode);
        wfExeCtxBuilder.setAdditionalInformationProvidedAtStart(workflowPage.getAdditionalInformation());
        WorkflowExecutionContext workflowExecutionContext = wfExeCtxBuilder.build();
        
        WorkflowExecutionService workflowExecutionService = serviceRegistryAccess.getService(WorkflowExecutionService.class);
        final WorkflowExecutionInformation wfExeInfo = workflowExecutionService.execute(workflowExecutionContext);

        // before starting the workflow, ensure that the console model is initialized
        // so that no console output gets lost; this is lazily initialized here
        // so the application startup is not slowed down
        serviceRegistryAccess.getService(ConsoleRowModelService.class).ensureConsoleCaptureIsInitialized();
        InputModel.ensureInputCaptureIsInitialized();

        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                new OpenReadOnlyWorkflowRunEditorAction(wfExeInfo).run();
            }

        });
    }
}

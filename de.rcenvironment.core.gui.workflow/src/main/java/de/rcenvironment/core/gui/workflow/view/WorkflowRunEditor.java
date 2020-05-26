/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.spi.SingleWorkflowStateChangeListener;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.UncompletedJobsShutdownListener;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorHelpContextProvider;
import de.rcenvironment.core.gui.workflow.editor.WorkflowScalableFreeformRootEditPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowRunEditorEditPartFactory;
import de.rcenvironment.core.gui.workflow.view.outline.OutlineView;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Graphical View for a running workflow instance.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Oliver Seebach
 * @author Jan Flink
 */
public class WorkflowRunEditor extends GraphicalEditor implements ITabbedPropertySheetPageContributor,
    SingleWorkflowStateChangeListener {

    private static final Log LOG = LogFactory.getLog(WorkflowRunEditor.class);

    private final DistributedNotificationService notificationService;

    private WorkflowStateNotificationSubscriber workflowStateChangeSubscriber;

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    private GraphicalViewer viewer;

    private WorkflowExecutionInformation wfExeInfo;

    private AtomicBoolean initialWorkflowStateSet = new AtomicBoolean(false);

    public WorkflowRunEditor() {
        setEditDomain(new DefaultEditDomain(this));
        registerWorkbenchListener();
        notificationService = ServiceRegistry.createAccessFor(this).getService(DistributedNotificationService.class);
    }

    private void registerWorkbenchListener() {
        PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {

            @Override
            public boolean preShutdown(IWorkbench workbench, boolean arg1) {
                // Close Workflow Run Editor programmatically on shutdown
                WorkflowRunEditor.this.getSite().getPage().closeEditor(WorkflowRunEditor.this, false);
                return true;
            }

            @Override
            public void postShutdown(IWorkbench workbench) {}
        });
    }

    public boolean isWorkflowExecutionInformationSet() {
        return wfExeInfo != null;
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalViewer().setEditPartFactory(new WorkflowRunEditorEditPartFactory());
        getGraphicalViewer().getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));

    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IPropertySheetPage.class) {
            if (tabbedPropertySheetPage == null || tabbedPropertySheetPage.getControl() == null
                || tabbedPropertySheetPage.getControl().isDisposed()) {
                tabbedPropertySheetPage = new TabbedPropertySheetPage(this);
            }
            return tabbedPropertySheetPage;
        } else if (type == IContextProvider.class) {
            return new WorkflowEditorHelpContextProvider(viewer);
        } else if (type == IContentOutlinePage.class) {
            return new OutlineView(getGraphicalViewer());
        } else if (type == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }
        return super.getAdapter(type);
    }

    private void updateTitle(String title) {
        setPartName(title);
    }

    /**
     * Retrieves the current state of the workflow and adds it to the title of the view.
     * 
     * @param workflowState new {@link WorkflowState}
     */
    public void updateTitle(WorkflowState workflowState) {
        updateTitle(wfExeInfo.getInstanceName() + ": " + workflowState.getDisplayName());
    }

    /**
     * Retrieves the current state of the workflow and sets the icon of the view accordingly.
     * 
     * @param state new {@link WorkflowState}
     */
    public void updateTabIcon(WorkflowState state) {
        Image stateImage = Activator.getInstance().getImageRegistry().get(state.name());
        if (stateImage != null) {
            setTitleImage(stateImage);
        } else {
            setTitleImage(Activator.getInstance().getImageRegistry().get(WorkflowState.UNKNOWN.name()));
        }
    }

    /**
     * Called externally from WorkflowListView to set a new workflow.
     * 
     * @param wei The workflow.
     */
    public void setWorkflowExecutionInformation(final WorkflowExecutionInformation wei) {
        this.wfExeInfo = wei;
        // FIXME: separate workflow execution information from workflow layout information to allow changes on gui side even if
        // backward compatibility is required - two following for-loops should be removed as soon as issue #0011902 is resolved or with
        // 7.0 as backward compatibility is not needed any longer -seid_do, April 2015
        if ((!wfExeInfo.getWorkflowDescription().getConnections().isEmpty()
            && wfExeInfo.getWorkflowDescription().getConnections().get(0).getBendpoints() == null)
            || (!wfExeInfo.getWorkflowDescription().getWorkflowLabels().isEmpty()
                && wfExeInfo.getWorkflowDescription().getWorkflowLabels().get(0).getLabelPosition() == null)) {
            wfExeInfo.getWorkflowDescription().setWorkflowLabels(new ArrayList<WorkflowLabel>());
            for (Connection connection : wfExeInfo.getWorkflowDescription().getConnections()) {
                connection.setBendpoints(new ArrayList<Location>());
            }
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Labels/custom connection paths",
                "Labels and/or custom connection paths cannot be displayed, as the controller was executed on an RCE instance <= 6.1, "
                    + "which neither supports labels nor custom connection paths.");
        }

        // set the model of the editor
        viewer.setContents(wei);
        workflowStateChangeSubscriber = new WorkflowStateNotificationSubscriber(WorkflowRunEditor.this,
            wei.getExecutionIdentifier());

        Job job = new Job(StringUtils.format("Initializing state of workflow '%s'", wei.getInstanceName())) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    notificationService.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeInfo.getExecutionIdentifier(),
                        workflowStateChangeSubscriber, wfExeInfo.getNodeId());
                } catch (RemoteOperationException e1) {
                    // TODO review: how to react on this?
                    LOG.error("Failed to subscribe for workflow state changes: " + e1.getMessage());
                    return Status.CANCEL_STATUS; // should be the closest to the "old" RTE behavior for now
                }

                if (!initialWorkflowStateSet.get()) {
                    WorkflowExecutionService wfExecutionService = ServiceRegistry.createAccessFor(WorkflowRunEditor.this)
                        .getService(WorkflowExecutionService.class);
                    try {
                        WorkflowState workflowState = wfExecutionService.getWorkflowState(wfExeInfo.getWorkflowExecutionHandle());
                        synchronized (WorkflowRunEditor.this) {
                            if (!initialWorkflowStateSet.get()) {
                                onWorkflowStateChanged(workflowState);
                            }
                        }
                    } catch (ExecutionControllerException | RemoteOperationException e) {
                        // TODO review: how to react on this?
                        LOG.error("Failed to subscribe for workflow state changes: " + e.getMessage());
                        return Status.CANCEL_STATUS; // should be the closest to the "old" RTE behavior for now
                    }
                }
                return Status.OK_STATUS;
            }
        };

        job.schedule();

    }

    @Override
    public void dispose() {
        Job job = new Job("Unsubscribing from workflow host") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                if (wfExeInfo != null) {
                    try {
                        notificationService.unsubscribe(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeInfo.getExecutionIdentifier(),
                            workflowStateChangeSubscriber, wfExeInfo.getNodeId());
                    } catch (RemoteOperationException e) {
                        LOG.error("Failed to unsubscribe workflow execution view from workflow host: " + e.getMessage());
                        return Status.CANCEL_STATUS;
                    }
                }
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return family == UncompletedJobsShutdownListener.MUST_BE_COMPLETED_ON_SHUTDOWN_JOB_FAMILY;
            }

        };
        job.setSystem(true);
        job.schedule();
        super.dispose();
    }

    @Override
    protected void initializeGraphicalViewer() {
        viewer = getGraphicalViewer();
        WorkflowScalableFreeformRootEditPart rootEditPart = new WorkflowScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        final ContextMenuProvider cmProvider = new WorkflowRunEditorContextMenuProvider(viewer);
        viewer.setContextMenu(cmProvider);

        tabbedPropertySheetPage = new TabbedPropertySheetPage(this);
        ZoomManager zoomManager = rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(zoomManager));
        getActionRegistry().registerAction(new ZoomOutAction(zoomManager));
        viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);

        viewer.getEditDomain().setDefaultTool(new PanningSelectionTool());
        viewer.getEditDomain().loadDefaultTool();

        setTitleImage(Activator.getInstance().getImageRegistry().get(WorkflowState.UNKNOWN.name()));
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public String getTitleToolTip() {
        return "";
    }

    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public String getContributorId() {
        return getSite().getId();
    }

    @Override
    public synchronized void onWorkflowStateChanged(final WorkflowState newState) {
        initialWorkflowStateSet.set(true);
        if (newState == WorkflowState.DISPOSING || newState == WorkflowState.DISPOSED) {
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    WorkflowRunEditor.this.getSite().getPage().closeEditor(WorkflowRunEditor.this, false);
                }
            });
        } else {
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    updateTitle(newState);
                    updateTabIcon(newState);
                }
            });
        }

    }

    @Override
    public void onWorkflowNotAliveAnymore(String errorMessage) {
        onWorkflowStateChanged(WorkflowState.UNKNOWN);
    }

    public GraphicalViewer getViewer() {
        return viewer;
    }

}

/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.help.IContextProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowStateChangeListener;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.UncompletedJobsShutdownListener;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorHelpContextProvider;
import de.rcenvironment.core.gui.workflow.parts.ReadOnlyEditPartFactory;
import de.rcenvironment.core.notification.SimpleNotificationService;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Graphical View for a running workflow instance.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Oliver Seebach
 */
public class ReadOnlyWorkflowRunEditor extends GraphicalEditor implements ITabbedPropertySheetPageContributor, WorkflowStateChangeListener {

    private static final Log LOG = LogFactory.getLog(ReadOnlyWorkflowRunEditor.class);

    private SimpleNotificationService sns = new SimpleNotificationService();

    private WorkflowStateNotificationSubscriber workflowStateChangeSubscriber = new WorkflowStateNotificationSubscriber(this);
    
    private TabbedPropertySheetPage tabbedPropertySheetPage;

    private GraphicalViewer viewer;

    private WorkflowExecutionInformation wfExeInfo;

    private ZoomManager zoomManager;
    
    private Boolean initialWorkflowStateSet = false;

    public ReadOnlyWorkflowRunEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalViewer().setEditPartFactory(new ReadOnlyEditPartFactory());
        getGraphicalViewer().getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));

    }
    
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IPropertySheetPage.class) {
            return tabbedPropertySheetPage;
        } else if (type == IContextProvider.class) {
            return new WorkflowEditorHelpContextProvider(viewer);
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
        // set the model of the editor
        viewer.setContents(wei);

        Job job = new Job(String.format("Initializing state of workflow '%s'", wei.getInstanceName())) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                sns.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeInfo.getExecutionIdentifier(),
                    workflowStateChangeSubscriber, wfExeInfo.getNodeId());

                if (!initialWorkflowStateSet) {
                    // TODO set initial state if no new state received yet
                    ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
                    WorkflowExecutionService executionService = serviceRegistryAccess.getService(WorkflowExecutionService.class);
                    try {
                        final WorkflowState state = executionService.getWorkflowState(wfExeInfo.getExecutionIdentifier(),
                            wei.getNodeId());
                        synchronized (initialWorkflowStateSet) {
                            if (!initialWorkflowStateSet) {
                                onNewWorkflowState(wfExeInfo.getExecutionIdentifier(), state);
                            }
                        }
                    } catch (CommunicationException e) {
                        LOG.error(String.format("Getting state of workflow '%s' failed", wfExeInfo.getInstanceName()));
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
                    sns.unsubscribe(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeInfo.getExecutionIdentifier(),
                        workflowStateChangeSubscriber, wfExeInfo.getNodeId());
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
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        final ContextMenuProvider cmProvider = new ReadOnlyWorkflowRunEditorContextMenuProvider(viewer);
        viewer.setContextMenu(cmProvider);

        tabbedPropertySheetPage = new TabbedPropertySheetPage(this);
        zoomManager = ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();

        viewer.getControl().addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseScrolled(MouseEvent arg0) {
                int notches = arg0.count;
                if (notches < 0) {
                    zoomManager.zoomOut();
                } else {
                    zoomManager.zoomIn();
                }

            }
        });
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
    public synchronized void onNewWorkflowState(String workflowIdentifier, final WorkflowState newState) {
        initialWorkflowStateSet = true;
        if (newState == WorkflowState.DISPOSING || newState == WorkflowState.DISPOSED) {
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    ReadOnlyWorkflowRunEditor.this.getSite().getPage().closeEditor(ReadOnlyWorkflowRunEditor.this, false);
                }
            });
        } else {
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    updateTitle(newState);
                    updateTabIcon(newState);
                }
            });
        }
        
    }

}

/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.list;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.communication.management.WorkflowHostSetListener;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.spi.MultipleWorkflowsStateChangeListener;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.view.OpenReadOnlyWorkflowRunEditorAction;
import de.rcenvironment.core.notification.SimpleNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.BatchAggregator;
import de.rcenvironment.core.utils.common.concurrent.BatchAggregator.BatchProcessor;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * This view shows all running workflows.
 * 
 * @author Heinrich Wendel
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class WorkflowListView extends ViewPart implements MultipleWorkflowsStateChangeListener {

    // the maximum number of ConsoleRows to aggregate to a single batch
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final int MAX_BATCH_SIZE = 500;

    // the maximum time a ConsoleRow may be delayed by batch aggregation
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final long MAX_BATCH_LATENCY_MSEC = 500;

    // guarded by synchronization on itself
    private final List<String> idsOfRemoteWorkflowsSubscribedFor = new ArrayList<String>();

    // guarded by synchronization on itself
    private final Set<NodeIdentifier> nodesSubscribedForNewWorkflows = new HashSet<NodeIdentifier>();

    private final WorkflowStateNotificationSubscriber workflowStateChangeListener =
        new WorkflowStateNotificationSubscriber(this);

    private final SimpleNotificationService sns = new SimpleNotificationService();

    private TableViewer viewer;

    private Table table;

    private WorkflowInformationColumnSorter columnSorter;

    private Action pauseAction;

    private Action resumeAction;

    private Action cancelAction;

    private Action disposeAction;

    private Display display;

    private ServiceRegistryPublisherAccess serviceRegistryPublisherAccess;

    private WorkflowExecutionService workflowExecutionService;

    private Object syncUpdateLock = new Object();

    private final BatchAggregator<Set<WorkflowExecutionInformation>> batchAggregator;

    public WorkflowListView() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        workflowExecutionService = serviceRegistryAccess.getService(WorkflowExecutionService.class);
        serviceRegistryPublisherAccess = ServiceRegistry.createPublisherAccessFor(this);

        BatchProcessor<Set<WorkflowExecutionInformation>> batchProcessor = new BatchAggregator
            .BatchProcessor<Set<WorkflowExecutionInformation>>() {

                @Override
                public void processBatch(final List<Set<WorkflowExecutionInformation>> batch) {
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            refresh(batch.get(batch.size() - 1));
                        }
                    });
                }

            };
        batchAggregator = new BatchAggregator<Set<WorkflowExecutionInformation>>(MAX_BATCH_SIZE, MAX_BATCH_LATENCY_MSEC, batchProcessor);
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * @param display
     */
    private void registerWorkflowHostSetListener() {

        serviceRegistryPublisherAccess.registerService(WorkflowHostSetListener.class, new WorkflowHostSetListener() {

            @Override
            public void onReachableWorkflowHostsChanged(Set<NodeIdentifier> reachableWfHosts, Set<NodeIdentifier> addedWfHosts,
                Set<NodeIdentifier> removedWfHosts) {
                updateSubscriptionsForNewlyCreatedWorkflows();
                synchronized (syncUpdateLock) {
                    final Set<WorkflowExecutionInformation> wis = updateWorkflowInformations();
                    if (!table.isDisposed()) {
                        batchAggregator.enqueue(wis);
                    }
                }

            }
        });
    }

    @Override
    public void createPartControl(Composite parent) {

        display = parent.getShell().getDisplay();

        viewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);

        viewer.getControl().addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent event) {}

            @Override
            public void keyPressed(KeyEvent event) {
                if (event.stateMask == SWT.CTRL && event.keyCode == 'a') {
                    viewer.getTable().selectAll();
                    getSite().getSelectionProvider().setSelection(viewer.getSelection());
                    updateSelectedWorkflowState();
                }

                if (event.keyCode == SWT.DEL) {

                    if (disposeAction.isEnabled()) {
                        disposeAction.run();

                    }
                }
            }
        });

        table = viewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        columnSorter = new WorkflowInformationColumnSorter();
        viewer.setSorter(columnSorter);

        String[] titles = {
            Messages.name, Messages.status, "Controller", "Start", "Started From", "Comment" };
        final int width = 150;

        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
            final TableColumn column = viewerColumn.getColumn();
            column.setText(titles[i]);
            column.setWidth(width);
            column.setResizable(true);
            column.setMoveable(true);
            column.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    columnSorter.setColumn(index);
                    int direction = viewer.getTable().getSortDirection();

                    if (viewer.getTable().getSortColumn() == column) {
                        if (direction == SWT.UP) {
                            direction = SWT.DOWN;
                        } else {
                            direction = SWT.UP;
                        }
                    } else {
                        direction = SWT.UP;
                    }
                    viewer.getTable().setSortDirection(direction);
                    viewer.getTable().setSortColumn(column);

                    viewer.refresh();
                }
            });
        }

        // add toolbar actions (right top of view)
        for (Action action : createToolbarActions()) {
            action.setEnabled(false);
            getViewSite().getActionBars().getToolBarManager().add(action);
        }

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                WorkflowExecutionInformation wi = (WorkflowExecutionInformation) ((IStructuredSelection)
                    viewer.getSelection()).getFirstElement();

                if (wi == null) {
                    return;
                }

                new OpenReadOnlyWorkflowRunEditorAction(wi).run();
            }
        });

        table.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent eve) {
                widgetDefaultSelected(eve);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent eve) {
                updateSelectedWorkflowState();
            }
        });

        Job job = new Job(Messages.workflows) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.fetchingWorkflows, 7);
                    // subscribe to all state notifications of the local node
                    try {
                        sns.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID + ".*", workflowStateChangeListener, null);
                    } catch (RemoteOperationException e) {
                        LogFactory.getLog(getClass()).error(
                            "Failed to set up remote subscriptions; the workflow list will not update properly: " + e.getMessage());
                        // TODO review: anything else that can be done except continuing with broken updates? - misc_ro, April 2015
                    }
                    monitor.worked(2);
                    // subscribe to get informed about new workflows created to refresh and fetch them
                    updateSubscriptionsForNewlyCreatedWorkflows();
                    monitor.worked(3);

                    monitor.worked(2);
                    final Set<WorkflowExecutionInformation> wis = updateWorkflowInformations();
                    display.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            refresh(wis);
                        }
                    });
                    return Status.OK_STATUS;
                } finally {
                    monitor.done();
                }
            };
        };
        job.setUser(true);
        job.schedule();

        hookContextMenu();
        registerWorkflowHostSetListener();
    }

    private void hookContextMenu() {
        MenuManager menuManager = new MenuManager();
        menuManager.add(pauseAction);
        menuManager.add(resumeAction);
        menuManager.add(cancelAction);
        menuManager.add(disposeAction);
        Menu menu = menuManager.createContextMenu(viewer.getTable());
        viewer.getTable().setMenu(menu);
        getSite().registerContextMenu(menuManager, viewer);
        getSite().setSelectionProvider(viewer);
    }

    /**
     * Refresh the contents of the table viewer.
     * 
     * @param wis workflow informations to consider
     */
    public void refresh(Set<WorkflowExecutionInformation> wis) {
        // ignore refresh request in case the table widget is already disposed
        if (table.isDisposed()) {
            return;
        }

        viewer.setContentProvider(new WorkflowInformationContentProvider());
        viewer.setLabelProvider(new WorkflowInformationLabelProvider());
        viewer.setInput(wis);
        getSite().setSelectionProvider(viewer);

        updateSelectedWorkflowState();
    }

    /**
     * @return workflow informations viewer must consider
     */
    public Set<WorkflowExecutionInformation> updateWorkflowInformations() {

        // synchronize to avoid needless/duplicate subscriptions
        synchronized (idsOfRemoteWorkflowsSubscribedFor) {

            Set<WorkflowExecutionInformation> wis = workflowExecutionService.getWorkflowExecutionInformations(true);
            // subscribe to all new remote ones in parallel and fetch their current states
            CallablesGroup<Void> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Void.class);
            List<String> alreadySubscribedWiIds = new ArrayList<String>(idsOfRemoteWorkflowsSubscribedFor);
            idsOfRemoteWorkflowsSubscribedFor.clear();
            for (final WorkflowExecutionInformation wi : wis) {
                final String executionId = wi.getExecutionIdentifier();
                idsOfRemoteWorkflowsSubscribedFor.add(executionId);
                if (!alreadySubscribedWiIds.contains(executionId)) {
                    callablesGroup.add(new Callable<Void>() {

                        @Override
                        @TaskDescription("Subscribe to new workflow")
                        public Void call() throws Exception {
                            sns.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID + executionId,
                                workflowStateChangeListener, wi.getNodeId());
                            WorkflowStateModel.getInstance().setState(wi.getExecutionIdentifier(), wi.getWorkflowState());

                            return null;
                        }
                    });
                }
            }
            callablesGroup.executeParallel(new AsyncExceptionListener() {

                @Override
                public void onAsyncException(Exception e) {
                    LogFactory.getLog(getClass()).warn("Asynchronous exception while subscribing to a new workflow");
                }
            });

            return wis;
        }
    }

    @Override
    public void setFocus() {
        table.setFocus();
        updateSelectedWorkflowState();
    }

    private Action[] createToolbarActions() {

        pauseAction = new WorflowLifeCycleAction(Messages.pause, Activator.getInstance().getImageRegistry()
            .getDescriptor(WorkflowState.PAUSED.name())) {

            @Override
            protected void performAction(WorkflowExecutionInformation wfExeInfo) throws ExecutionControllerException,
                RemoteOperationException {
                workflowExecutionService.pause(wfExeInfo.getExecutionIdentifier(), wfExeInfo.getNodeId());
            }

            @Override
            protected String getActionAsString() {
                return "Pausing";
            }

        };

        resumeAction = new WorflowLifeCycleAction(Messages.resume, Activator.getInstance().getImageRegistry()
            .getDescriptor(WorkflowState.RESUMING.name())) {

            @Override
            protected void performAction(WorkflowExecutionInformation wfExeInfo) throws ExecutionControllerException,
                RemoteOperationException {
                workflowExecutionService.resume(wfExeInfo.getExecutionIdentifier(), wfExeInfo.getNodeId());
            }

            @Override
            protected String getActionAsString() {
                return "Resuming";
            }
        };

        cancelAction = new WorflowLifeCycleAction(Messages.cancel, Activator.getInstance().getImageRegistry()
            .getDescriptor(WorkflowState.CANCELLED.name())) {

            @Override
            protected void performAction(WorkflowExecutionInformation wfExeInfo) throws ExecutionControllerException,
                RemoteOperationException {
                workflowExecutionService.cancel(wfExeInfo.getExecutionIdentifier(), wfExeInfo.getNodeId());
            }

            @Override
            protected String getActionAsString() {
                return "Cancelling";
            }
        };

        disposeAction = new WorflowLifeCycleAction(Messages.dispose, ImageDescriptor.createFromImage(
            ImageManager.getInstance().getSharedImage(StandardImages.REMOVE_16))) {

            @Override
            protected void performAction(WorkflowExecutionInformation wfExeInfo) throws ExecutionControllerException,
                RemoteOperationException {
                workflowExecutionService.dispose(wfExeInfo.getExecutionIdentifier(), wfExeInfo.getNodeId());
            }

            @Override
            protected String getActionAsString() {
                return "Disposing";
            }
        };

        return new Action[] { pauseAction, resumeAction, cancelAction, disposeAction };
    }

    /**
     * Abstract class for lifecylce actions.
     * 
     * @author Doreen Seider
     */
    private abstract class WorflowLifeCycleAction extends Action {

        protected WorflowLifeCycleAction(String text, ImageDescriptor image) {
            super(text, image);
        }

        @Override
        public void run() {
            @SuppressWarnings("unchecked") final List<Object> selection = ((StructuredSelection) viewer.getSelection()).toList();
            Job job = new Job(getActionAsString() + " workflow(s)") {

                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    CallablesGroup<Void> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Void.class);
                    for (Object o : selection) {
                        WorkflowExecutionInformation wfExeInfo = (WorkflowExecutionInformation) o;
                        try {
                            performAction(wfExeInfo);
                        } catch (ExecutionControllerException | RemoteOperationException e) {
                            LogFactory.getLog(WorkflowListView.class).error(
                                StringUtils.format("%s workflow failed", getActionAsString()), e);
                        }
                    }
                    callablesGroup.executeParallel(new AsyncExceptionListener() {

                        @Override
                        public void onAsyncException(Exception e) {
                            LogFactory.getLog(WorkflowListView.class).error(
                                StringUtils.format("%s workflow failed", getActionAsString()), e);
                        }
                    });
                    final Set<WorkflowExecutionInformation> wfExeInfos = updateWorkflowInformations();
                    try {
                        table.getDisplay().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                updateSelectedWorkflowState();
                                refresh(wfExeInfos);
                            }
                        });
                    } finally {
                        monitor.done();
                    }
                    return Status.OK_STATUS;
                };
            };
            job.setUser(false);
            job.schedule();
        }

        protected abstract void performAction(WorkflowExecutionInformation wfExeInfo) throws ExecutionControllerException,
            RemoteOperationException;

        protected abstract String getActionAsString();
    }

    private void updateSubscriptionsForNewlyCreatedWorkflows() {
        synchronized (nodesSubscribedForNewWorkflows) {
            CallablesGroup<NodeIdentifier> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(NodeIdentifier.class);

            ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
            Set<NodeIdentifier> nodes = registryAccess.getService(WorkflowHostService.class).getWorkflowHostNodesAndSelf();
            for (final NodeIdentifier node : nodes) {
                if (!nodesSubscribedForNewWorkflows.contains(node)) {
                    nodesSubscribedForNewWorkflows.add(node);
                    callablesGroup.add(new Callable<NodeIdentifier>() {

                        @Override
                        @TaskDescription("Distributed subscriptions for newly created workflow notifications")
                        public NodeIdentifier call() throws Exception {
                            sns.subscribe(WorkflowConstants.NEW_WORKFLOW_NOTIFICATION_ID, workflowStateChangeListener, node);
                            return node;
                        }
                    });
                }
            }
            List<NodeIdentifier> nodesAdded = callablesGroup.executeParallel(new AsyncExceptionListener() {

                @Override
                public void onAsyncException(Exception e) {
                    final Log log = LogFactory.getLog(getClass());
                    if (e.getCause() == null) {
                        // log a compressed message; this includes RemoteOperationExceptions, which (by design) never have a "cause"
                        log.warn("Asynchronous exception during parallel subscriptions for newly created workflow notifications: "
                            + e.toString());
                    } else {
                        // on unexpected errors, log the full stacktrace
                        log.warn("Asynchronous exception during parallel subscriptions for newly created workflow notifications", e);
                    }
                }
            });
            nodesSubscribedForNewWorkflows.retainAll(nodesAdded);
        }
    }

    private void updateSelectedWorkflowState() {
        setAllIconsEnabled(true);
        if (viewer.getSelection() != null) {
            if (((IStructuredSelection) viewer.getSelection()).size() > 0) {
                for (Object o : ((IStructuredSelection) viewer.getSelection()).toList()) {
                    WorkflowExecutionInformation wi = (WorkflowExecutionInformation) o;
                    final WorkflowState workflowState = WorkflowStateModel.getInstance().getState(wi.getExecutionIdentifier());
                    if (workflowState == WorkflowState.RUNNING || workflowState == WorkflowState.PREPARING) {
                        resumeAction.setEnabled(false);
                        disposeAction.setEnabled(false);
                    } else if (workflowState == WorkflowState.PAUSED) {
                        pauseAction.setEnabled(false);
                        disposeAction.setEnabled(false);
                    } else if (workflowState == WorkflowState.FINISHED
                        || workflowState == WorkflowState.CANCELLED
                        || workflowState == WorkflowState.FAILED) {
                        pauseAction.setEnabled(false);
                        resumeAction.setEnabled(false);
                        cancelAction.setEnabled(false);
                    } else {
                        setAllIconsEnabled(false);
                    }
                }
            } else {
                setAllIconsEnabled(false);
            }
        }
    }

    private void setAllIconsEnabled(boolean enabled) {
        pauseAction.setEnabled(enabled);
        resumeAction.setEnabled(enabled);
        cancelAction.setEnabled(enabled);
        disposeAction.setEnabled(enabled);
    }

    @Override
    public void onWorkflowStateChanged(String wfExecutionId, WorkflowState newState) {
        if (newState != null) {
            WorkflowStateModel.getInstance().setState(wfExecutionId, newState);
        }
        synchronized (syncUpdateLock) {
            final Set<WorkflowExecutionInformation> wis = updateWorkflowInformations();
            batchAggregator.enqueue(wis);
        }
    }

}

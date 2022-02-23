/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.WorkflowHostSetListener;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.EndpointContentProvider;
import de.rcenvironment.core.gui.workflow.editor.connections.EndpointTreeViewer;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Property section for displaying and editing inputs.
 * 
 * @author Doreen Seider
 */
public abstract class AbstractInputSection extends AbstractPropertySection {

    private static final long REFRESH_INTERVAL = 1000;

    private static final int COLUMN_WIDTH_ONE = 250;

    private static final int COLUMN_WIDTH_THREE = 50;

    protected WorkflowExecutionInformation wfExeInfo;

    protected EndpointTreeViewer inputTreeViewer;

    private Composite parent;

    private List<TreeEditor> treeEditors;

    private InputModel inputModel;

    private Display display;

    private ServiceRegistryPublisherAccess serviceRegistryAccess;

    private CountDownLatch modelInitializedLatch;

    private ScheduledFuture<?> refreshFuture;

    /**
     * Periodically refreshes this section.
     * 
     * @author Doreen Seider
     */
    private class RefreshTask implements Runnable {

        @Override
        public void run() {
            if (inputTreeViewer != null && !inputTreeViewer.getTree().isDisposed()) {
                inputTreeViewer.getTree().getDisplay().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (inputModel != null) {
                            refreshTable();
                            InputQueueDialogController inputQueueDialogController = InputQueueDialogController.getInstance();
                            if (inputQueueDialogController != null) {
                                inputQueueDialogController.redrawTable();
                            }
                        }
                    }
                });
            }
        }
    }

    public AbstractInputSection() {
        treeEditors = new ArrayList<TreeEditor>();

        modelInitializedLatch = new CountDownLatch(1);

        Job job = new Job("Opening inputs view") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                inputModel = InputModel.getInstance();
                modelInitializedLatch.countDown();
                return Status.OK_STATUS;
            }
        };

        job.schedule();
    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);

        retrieveWorkflowInformation(part, selection);
        initializeTreeViewer(part, selection);

        inputTreeViewer.expandAll();
        setInputQueueButton(inputTreeViewer.getTree());
        refresh();
    }

    protected abstract void retrieveWorkflowInformation(IWorkbenchPart part, ISelection selection);

    protected abstract void initializeTreeViewer(IWorkbenchPart part, ISelection selection);

    @Override
    public void createControls(final Composite aParent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createControls(parent, aTabbedPropertySheetPage);
        parent = aParent;
        display = parent.getShell().getDisplay();

        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        parent.setLayout(gridLayout);

        inputTreeViewer = new EndpointTreeViewer(parent, SWT.FULL_SELECTION);
        inputTreeViewer.setContentProvider(new EndpointContentProvider(EndpointType.INPUT));
        final Tree endpointTree = inputTreeViewer.getTree();
        endpointTree.setHeaderVisible(true);
        TreeColumn inputColumn = new TreeColumn(endpointTree, SWT.LEFT);
        inputColumn.setText(Messages.inputs);
        inputColumn.setWidth(COLUMN_WIDTH_ONE);
        TreeColumn currentInputColumn = new TreeColumn(endpointTree, SWT.CENTER);
        currentInputColumn.setText(Messages.latestInput);
        currentInputColumn.setWidth(COLUMN_WIDTH_ONE);
        TreeColumn inputQueueColumn = new TreeColumn(endpointTree, SWT.LEFT);
        inputQueueColumn.setText(Messages.inputQueue);
        inputQueueColumn.setWidth(COLUMN_WIDTH_THREE);

        endpointTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        endpointTree.setLinesVisible(true);

        registerWorkflowHostSetListener();

        scheduleRefreshTimer();
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * @param display
     */
    private void registerWorkflowHostSetListener() {

        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        serviceRegistryAccess.registerService(WorkflowHostSetListener.class, new WorkflowHostSetListener() {

            @Override
            public void onReachableWorkflowHostsChanged(Set<InstanceNodeSessionId> reachableWfHosts,
                Set<InstanceNodeSessionId> addedWfHosts, Set<InstanceNodeSessionId> removedWfHosts) {
                try {
                    modelInitializedLatch.await();
                } catch (InterruptedException e) {
                    // TODO better handling?
                    throw new RuntimeException("Interrupted while waiting for input model creation to complete", e);
                }
                inputModel.updateSubscriptions();
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (inputTreeViewer != null && !inputTreeViewer.getTree().isDisposed()) {
                            inputTreeViewer.refresh();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void dispose() {
        if (refreshFuture != null) {
            refreshFuture.cancel(true);
        }
        if (serviceRegistryAccess != null) {
            serviceRegistryAccess.dispose();
        }
        super.dispose();
    }

    /**
     * Schedules the refresh timer responsible for refreshing the view every 500 milliseconds.
     */
    public void scheduleRefreshTimer() {
        refreshFuture = ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedInterval("Refresh inputs processed by components",
            new RefreshTask(), REFRESH_INTERVAL);
    }

    /**
     * Cancels the refresh timer responsible for refreshing the view every 500 milliseconds.
     */
    public void cancelRefreshTimer() {
        refreshFuture.cancel(true);
    }

    /**
     * Refrehes the table. Same action timer does.
     */
    public void refreshTable() {
        if (inputTreeViewer != null && inputTreeViewer.getTree() != null && !inputTreeViewer.getTree().isDisposed()) {
            inputTreeViewer.getControl().setRedraw(false);
            inputTreeViewer.refresh();
            inputTreeViewer.expandAll();
            inputTreeViewer.getControl().setRedraw(true);
            setInputQueueButton(inputTreeViewer.getTree());
            inputTreeViewer.getControl().redraw();
        }
    }

    private void setInputQueueButton(final Tree tree) {
        for (TreeEditor treeEditor : treeEditors) {
            Control oldEditor = treeEditor.getEditor();
            if (oldEditor != null) {
                oldEditor.dispose();
            }
        }
        treeEditors.clear();
        for (final TreeItem treeItem : inputTreeViewer.getTree().getItems()) {
            setInputQueueButton(treeItem);
        }
    }

    private void setInputQueueButton(final TreeItem treeItem) {
        if (treeItem.getItemCount() > 0) {
            for (final TreeItem childTreeItem : treeItem.getItems()) {
                setInputQueueButton(childTreeItem);
            }
        } else {
            TreeEditor treeEditor = new TreeEditor(inputTreeViewer.getTree());
            Button button = new Button(inputTreeViewer.getTree(), SWT.PUSH);
            button.setText("...");
            button.computeSize(SWT.DEFAULT, inputTreeViewer.getTree().getItemHeight());
            treeEditor.grabHorizontal = true;
            treeEditor.minimumHeight = button.getSize().y;
            treeEditor.minimumWidth = button.getSize().x;

            treeEditor.setEditor(button, treeItem, 2);

            treeEditors.add(treeEditor);

            button.addSelectionListener(new SelectionAdapter() {

                private TreeItem item = treeItem;

                @Override
                public void widgetSelected(SelectionEvent event) {
                    openInputDialog(item);
                }

            });
        }
    }

    protected abstract void openInputDialog(TreeItem item);
}

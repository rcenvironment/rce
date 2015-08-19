/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.view.internal;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.gui.cluster.configuration.internal.ClusterConnectionConfigurationDialogsController;
import de.rcenvironment.core.gui.cluster.internal.ErrorMessageDialogFactory;
import de.rcenvironment.core.utils.cluster.ClusterService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * View monitoring information about cluster jobs.
 * 
 * @author Doreen Seider
 */
public class ClusterJobMonitorView extends ViewPart {

    private static final String CONNECTING_TO_CLUSTER_FAILED = "Connecting to cluster failed";
    
    private static final String FETCHING_CLUSTER_JOB_INFORMATION_FAILED = "Fetching cluster job information failed";

    private static final int COLUMN_WIDTH = 120;

    private static final int COLUMN_WIDTH_JOBSTATE = 100;

    private static final int NO_SPACE = 0;

    private static final int PLATFORM_WIDTH = 250;

    private static final int TEXT_WIDTH = PLATFORM_WIDTH;
    
    private static final int DELAY = 30000;

    private static final Log LOGGER = LogFactory.getLog(ClusterJobMonitorView.class);

    private Button othersCheckbox;

    private Button runningCheckbox;

    private Button queuedCheckbox;

    private Combo connectedConfigurationNameCombo;

    private ClusterJobInformationTableFilter tableRowFilter;

    private ClusterJobInformationTableColumnSorter tableColumnSorter;

    private TableViewer jobInformationTableViewer;

    private Text searchText;
    
    private Composite parent;
   
    private Timer getUpdateTimer;
    
    private volatile boolean getUpdateTimerTaskScheduled = false;
    
    private final Action connectAction = new Action(Messages.connectToolTip, ImageDescriptor.createFromURL(
        ClusterJobMonitorView.class.getResource("/resources/icons/connect16.png"))) {

        @Override
        public void run() {
            final ClusterConnectionConfigurationDialogsController controller = new ClusterConnectionConfigurationDialogsController(parent);
            final ClusterConnectionInformation connectionInformation = controller.openClusterConnectionSelectionDialog();
            
            if (connectionInformation != null) {
                
                BusyIndicator.showWhile(jobInformationTableViewer.getTable().getDisplay(), new Runnable() {
                    
                    @Override
                    public void run() {
                        String clusterConfigurationName = controller.getClusterConfigurationName();
                        ClusterService clusterInformationService = controller.getClusterJobInformationService();
                        
                        if (updateModelForFirstTime(clusterConfigurationName, clusterInformationService, connectionInformation)) {
                            refreshView(clusterConfigurationName);                            
                            scheduleGetUpdateOfClusterJobInformationTimerIfNeeded();                            
                        }
                        
                    }
                });
                
            }
        }
    };
    
    private final Action disconnectAction = new Action(Messages.disconnectToolTip, ImageDescriptor.createFromURL(
        ClusterJobMonitorView.class.getResource("/resources/icons/disconnect16.png"))) {

        @Override
        public void run() {
            
            String clusterConfigurationName = connectedConfigurationNameCombo.getItem(connectedConfigurationNameCombo.getSelectionIndex());

            ClusterJobInformationModel model = ClusterJobInformationModel.getInstance();
            model.removeConnectedCluster(clusterConfigurationName);
            
            refreshConnectedClusterComboAndToolBar(null);
            
            clusterConfigurationName = connectedConfigurationNameCombo.getItem(connectedConfigurationNameCombo.getSelectionIndex());
            
            model.setSelectedConnectedConfigurationName(clusterConfigurationName);
            
            refreshClusterJobInformationTableViewer();

            cancelUpdateClusterJobInformationTimerIfNeeded();
        }
    };
    
    private final Action refreshAction = new Action(Messages.refreshToolTip, ImageDescriptor.createFromURL(
        ClusterJobMonitorView.class.getResource("/resources/icons/refresh.gif"))) {

        @Override
        public void run() {
            BusyIndicator.showWhile(jobInformationTableViewer.getTable().getDisplay(), new Runnable() {
                @Override
                public void run() {
                    getUpdateOfClusterJobInformation();
                    refreshClusterJobInformationTableViewer();
                }
            });
        }
    };
    
    private final Action informationAction = new Action(Messages.informationToolTip, ImageDescriptor.createFromURL(
        ClusterJobMonitorView.class.getResource("/resources/icons/information.gif"))) {

        @Override
        public void run() {
            ClusterJobInformationModel model = ClusterJobInformationModel.getInstance();
            String connectedConfigurationName = connectedConfigurationNameCombo
                .getItem(connectedConfigurationNameCombo.getSelectionIndex());
            ClusterConnectionInformation connectionInformation = model.getClusterConnectionInformation(connectedConfigurationName);
            MessageDialog dialog = new MessageDialog(parent.getShell(), Messages.informationDialogTitle, null,
                StringUtils.format(Messages.informationDialogMessage,
                    connectionInformation.getHost(), connectionInformation.getUsername(), connectionInformation.getConnectedDate(),
                    connectionInformation.getLastUpdateDate(),
                    connectionInformation.getUpdateInterval()),
                MessageDialog.INFORMATION, new String[] { Messages.ok }, 0);
            dialog.open();
        }
    };

    @Override
    public void createPartControl(Composite aParent) {
        this.parent = aParent;
        
        aParent.setLayout(new GridLayout(1, false));

        // filter = cluster selection, state selection, and search text field
        Composite filterComposite = new Composite(aParent, SWT.NONE);
        filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        filterComposite.setLayout(new RowLayout());

        createConnectedClusterListingArrangement(filterComposite);
        createLevelArrangement(filterComposite);
        createSearchArrangement(filterComposite);

        createTableArrangement(aParent);

        tableColumnSorter = new ClusterJobInformationTableColumnSorter();
        jobInformationTableViewer.setSorter(tableColumnSorter);

        tableRowFilter = new ClusterJobInformationTableFilter(this, jobInformationTableViewer);

        searchText.addKeyListener(tableRowFilter);

        queuedCheckbox.addSelectionListener(tableRowFilter);
        runningCheckbox.addSelectionListener(tableRowFilter);
        othersCheckbox.addSelectionListener(tableRowFilter);
        connectedConfigurationNameCombo.addSelectionListener(tableRowFilter);

        jobInformationTableViewer.addFilter(tableRowFilter);
        
        for (Action action : createToolbarActions()) {
            getViewSite().getActionBars().getToolBarManager().add(action);
        }
        
        String[] connectedConfigurationNames = ClusterJobInformationModel.getInstance().getConnectedConfigurationNames();
        boolean enabled = connectedConfigurationNames.length > 0
            && !connectedConfigurationNames[0].equals(ClusterJobInformationModel.NOT_CONNECTED);
        disconnectAction.setEnabled(enabled);
        refreshAction.setEnabled(enabled);
        informationAction.setEnabled(enabled);
        
    }

    @Override
    public void setFocus() {
        jobInformationTableViewer.getControl().setFocus();
        scheduleGetUpdateOfClusterJobInformationTimerIfNeeded();
    }
    
    @Override
    public void dispose() {
        cancelUpdateClusterJobInformationTimerIfNeeded();
    }
    
    public boolean getQueuedSelection() {
        return queuedCheckbox.getSelection();
    }

    public boolean getRunningSelection() {
        return runningCheckbox.getSelection();
    }

    public boolean getOthersSelection() {
        return othersCheckbox.getSelection();
    }

    public String getSelectedConnectedConfigurationName() {
        return connectedConfigurationNameCombo.getItem(connectedConfigurationNameCombo.getSelectionIndex());
    }

    public String getSearchText() {
        return searchText.getText();
    }
    
    private Action[] createToolbarActions() {
        return new Action[] {
            connectAction, disconnectAction, refreshAction, informationAction
        };
    }
    
    private void createLevelArrangement(Composite filterComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = NO_SPACE;
        filterComposite.setLayout(rowLayout);

        queuedCheckbox = new Button(filterComposite, SWT.CHECK);
        queuedCheckbox.setText(Messages.queuedFilter);
        queuedCheckbox.setSelection(true);

        runningCheckbox = new Button(filterComposite, SWT.CHECK);
        runningCheckbox.setText(Messages.runningFilter);
        runningCheckbox.setSelection(true);

        othersCheckbox = new Button(filterComposite, SWT.CHECK);
        othersCheckbox.setText(Messages.othersFilter);
        othersCheckbox.setSelection(true);

    }

    private void createConnectedClusterListingArrangement(Composite connectedClusterComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = NO_SPACE;
        connectedClusterComposite.setLayout(rowLayout);

        connectedConfigurationNameCombo = new Combo(connectedClusterComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        connectedConfigurationNameCombo.setLayoutData(new RowData(PLATFORM_WIDTH, SWT.DEFAULT));
        connectedConfigurationNameCombo.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                ClusterJobInformationModel.getInstance().setSelectedConnectedConfigurationName(
                    connectedConfigurationNameCombo.getItem(connectedConfigurationNameCombo.getSelectionIndex()));
                refreshClusterJobInformationTableViewer();
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        refreshConnectedClusterComboAndToolBar(null);
    }

    private void createSearchArrangement(Composite searchComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = 7;
        searchComposite.setLayout(rowLayout);

        searchText = new Text(searchComposite, SWT.SEARCH);
        searchText.setMessage(Messages.searchFilter);
        searchText.setSize(TEXT_WIDTH, SWT.DEFAULT);
        searchText.setLayoutData(new RowData(TEXT_WIDTH, SWT.DEFAULT));
    }

    private void createTableArrangement(Composite tableComposite) {
        tableComposite.setLayout(new GridLayout());

        jobInformationTableViewer = new TableViewer(tableComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
        jobInformationTableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // create table header and column styles
        String[] columnTitles = new String[] {
            Messages.columnJobId,
            Messages.columnJobName,
            Messages.columnUser,
            Messages.columnQueue,
            Messages.columnRemainingTime,
            Messages.columnStartTime,
            Messages.columnQueueTime,
            Messages.columnJobState };
        int[] bounds = { COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH,
            COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH_JOBSTATE };

        for (int i = 0; i < columnTitles.length; i++) {
            // for all columns

            final int index = i;
            final TableViewerColumn viewerColumn = new TableViewerColumn(jobInformationTableViewer, SWT.NONE);
            final TableColumn column = viewerColumn.getColumn();

            // set column properties
            column.setText(columnTitles[i]);
            column.setWidth(bounds[i]);
            column.setResizable(true);
            column.setMoveable(true);

            column.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    tableColumnSorter.setColumn(index);
                    int direction = jobInformationTableViewer.getTable().getSortDirection();

                    if (jobInformationTableViewer.getTable().getSortColumn() == column) {
                        if (direction == SWT.UP) {
                            direction = SWT.DOWN;
                        } else {
                            direction = SWT.UP;
                        }
                    } else {
                        direction = SWT.UP;
                    }
                    jobInformationTableViewer.getTable().setSortDirection(direction);
                    jobInformationTableViewer.getTable().setSortColumn(column);

                    refreshClusterJobInformationTableViewer();
                }
            });
        }

        jobInformationTableViewer.setContentProvider(new ClusterJobInformationContentProvider());
        jobInformationTableViewer.setLabelProvider(new ClusterJobInformationLabelProvider());
        jobInformationTableViewer.setInput(ClusterJobInformationModel.getInstance());

        // set table layout data
        final Table table = jobInformationTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Menu contextMenu = new Menu(tableComposite);
        final MenuItem killJobItem = new MenuItem(contextMenu, SWT.PUSH);
        killJobItem.setText(Messages.cancelJob);
        killJobItem.setEnabled(table.getSelectionCount() > 0);
        killJobItem.addSelectionListener(new KillClusterJobListenerListener(jobInformationTableViewer, refreshAction));
        jobInformationTableViewer.getControl().setMenu(contextMenu);

        table.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                killJobItem.setEnabled(table.getSelectionCount() > 0);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        

    }
        
    private synchronized void getUpdateOfClusterJobInformation() {
        try {
            ClusterJobInformationModel.getInstance().getUpdateFromCluster();
        } catch (final IOException e) {
            jobInformationTableViewer.getTable().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    LOGGER.error(FETCHING_CLUSTER_JOB_INFORMATION_FAILED, e);
                    disconnectAction.run();
                    ErrorMessageDialogFactory.createMessageDialogForFetchingFailure(parent).open();
                }
            });
            
        }
    }
    
    private boolean updateModelForFirstTime(String connectedClusterConfigurationName,  ClusterService clusterInformationService,
        ClusterConnectionInformation connectionInformation) {
        ClusterJobInformationModel model = ClusterJobInformationModel.getInstance();
        model.addConnectedCluster(connectedClusterConfigurationName, clusterInformationService);
        final int convertToSecondsQuotient = 1000;
        connectionInformation.setUpdateInterval(ClusterService.FETCH_INTERVAL / convertToSecondsQuotient);
        model.addClusterConnectionInformation(connectedClusterConfigurationName, connectionInformation);
        model.setSelectedConnectedConfigurationName(connectedClusterConfigurationName);
        try {
            model.getUpdateFromCluster();
        } catch (IOException e) {
            ErrorMessageDialogFactory.createMessageDialogForConnectionFailure(parent, e).open();
            LOGGER.error(CONNECTING_TO_CLUSTER_FAILED, e);
            model.removeConnectedCluster(connectedClusterConfigurationName);
            return false;
        }
        model.setSelectedConnectedConfigurationName(connectedClusterConfigurationName);
        return true;
    }

    private void refreshView(String connectedClusterConfigurationName) {
        refreshConnectedClusterComboAndToolBar(connectedClusterConfigurationName);
        refreshClusterJobInformationTableViewer();
    }
    
    private void refreshConnectedClusterComboAndToolBar(String connectedClusterConfigurationName) {
        connectedConfigurationNameCombo.setItems(ClusterJobInformationModel.getInstance().getConnectedConfigurationNames());
        
        if (connectedClusterConfigurationName != null && !connectedClusterConfigurationName.isEmpty()) {
            connectedConfigurationNameCombo.select(connectedConfigurationNameCombo.indexOf(connectedClusterConfigurationName));   
            disconnectAction.setEnabled(true);
            refreshAction.setEnabled(true);
            informationAction.setEnabled(true);
        } else {
            connectedConfigurationNameCombo.select(0);
            if (!isConnectedToAtLeastOneCluster()) {
                disconnectAction.setEnabled(false);
                refreshAction.setEnabled(false);
                informationAction.setEnabled(false);
            }
        }
    }

    private void refreshClusterJobInformationTableViewer() {
        jobInformationTableViewer.getTable().clearAll();
        jobInformationTableViewer.refresh();
    }
    
    private void scheduleGetUpdateOfClusterJobInformationTimerIfNeeded() {
        
        if (!getUpdateTimerTaskScheduled && isConnectedToAtLeastOneCluster()) {
            TimerTask getUpdateTimerTask = new TimerTask() {
                
                @Override
                public void run() {
                    Job job = new Job(Messages.updateJobTitle) {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                monitor.beginTask(Messages.updateJobMessage, 3);
                                monitor.worked(1);
                                getUpdateOfClusterJobInformation();
                                monitor.worked(2);
                                if (!jobInformationTableViewer.getTable().isDisposed()) {
                                    jobInformationTableViewer.getTable().getDisplay().asyncExec(new Runnable() {

                                        @Override
                                        public void run() {
                                            refreshClusterJobInformationTableViewer();
                                        }
                                    });                                    
                                }
                            } finally {
                                monitor.done();
                            }
                            return Status.OK_STATUS;
                        };
                    };
                    job.setUser(false);
                    job.schedule();
                    
                }
            };

            getUpdateTimer = new Timer("Get Update (Cluster Job Information) Timer", true);
            getUpdateTimer.schedule(getUpdateTimerTask, DELAY, ClusterService.FETCH_INTERVAL);
            getUpdateTimerTaskScheduled = true;
        }
    }
    
    private void cancelUpdateClusterJobInformationTimerIfNeeded() {
        if (getUpdateTimerTaskScheduled && !isConnectedToAtLeastOneCluster()) {
            getUpdateTimer.cancel();
            getUpdateTimerTaskScheduled = false;
        }
    }
    
    private boolean isConnectedToAtLeastOneCluster() {
        return !connectedConfigurationNameCombo.getItem(connectedConfigurationNameCombo.getSelectionIndex())
            .equals(ClusterJobInformationModel.NOT_CONNECTED);
    }
    
}

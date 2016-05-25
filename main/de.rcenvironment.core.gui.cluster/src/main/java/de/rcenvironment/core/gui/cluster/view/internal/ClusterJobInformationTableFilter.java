/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.view.internal;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;
import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;


/**
 * Listener for GUI-elements such as check box, drop down box, and text field to realize changes.
 * In case of changes it pushes to refresh displaying the data and organizes filtering table data. 
 * 
 * @author Doreen Seider
 */
public class ClusterJobInformationTableFilter extends ViewerFilter implements SelectionListener, KeyListener {

    private boolean queuedSetup;

    private boolean runningSetup;

    private boolean othersSetup;

    private ClusterJobMonitorView clusterJobMonitoringView;

    private TableViewer tableViewer;

    public ClusterJobInformationTableFilter(ClusterJobMonitorView loggingView, TableViewer tableViewer) {
        clusterJobMonitoringView = loggingView;
        this.tableViewer = tableViewer;

        updateTableView();
        
        tableViewer.getTable().setFocus();
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        boolean returnValue = false;

        if (element instanceof ClusterJobInformation) {
            ClusterJobInformation logEntry = (ClusterJobInformation) element;

            if (isLevelSelected(logEntry.getJobState())) {
                if (isSelectedBySearchTerm(logEntry.getJobId())) {
                    returnValue = true;
                } else if (isSelectedBySearchTerm(logEntry.getJobName())) {
                    returnValue = true;
                } else if (isSelectedBySearchTerm(logEntry.getUser())) {
                    returnValue = true;
                } else if (isSelectedBySearchTerm(logEntry.getQueue())) {
                    returnValue = true;
                } else if (isSelectedBySearchTerm(logEntry.getRemainingTime())) {
                    returnValue = true;
                } else if (isSelectedBySearchTerm(logEntry.getStartTime())) {
                    returnValue = true;
                } else if (isSelectedBySearchTerm(logEntry.getQueueTime())) {
                    returnValue = true;
                } else if (isSelectedBySearchTerm(logEntry.getJobState().toString())) {
                    returnValue = true;
                }
            }
        }

        return returnValue;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        updateTableView();
        tableViewer.getTable().setFocus();
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
    // do nothing
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        updateTableView();
    }

    private void updateTableView() {
        queuedSetup = clusterJobMonitoringView.getQueuedSelection();
        runningSetup = clusterJobMonitoringView.getRunningSelection();
        othersSetup = clusterJobMonitoringView.getOthersSelection();

        ClusterJobInformationModel.getInstance()
            .setSelectedConnectedConfigurationName(clusterJobMonitoringView.getSelectedConnectedConfigurationName());
        
        tableViewer.refresh();
    }

    private boolean isLevelSelected(ClusterJobState jobState) {
        boolean returnValue = false;

        if (ClusterJobState.Queued == jobState && queuedSetup) {
            returnValue = true;
        } else if (ClusterJobState.Running == jobState && runningSetup) {
            returnValue = true;
        } else if (ClusterJobState.Completed != jobState
            && ClusterJobState.Queued != jobState
            && ClusterJobState.Running != jobState && othersSetup) {
            returnValue = true;
        }

        return returnValue;
    }

    private boolean isSelectedBySearchTerm(String entryText) {
        boolean returnValue = false;

        String searchTerm = clusterJobMonitoringView.getSearchText();
        
        if (searchTerm == null || searchTerm.length() == 0) {
            returnValue = true;
        } else if (textMatchesSearchTerm(searchTerm, entryText)) {
            returnValue = true;
        }

        return returnValue;
    }

    private boolean textMatchesSearchTerm(String searchTerm, String entryText) {
        // search is case insensitive - see also method 'setSearchTerm'
        return entryText.toLowerCase().contains(searchTerm.toLowerCase());
    }
}

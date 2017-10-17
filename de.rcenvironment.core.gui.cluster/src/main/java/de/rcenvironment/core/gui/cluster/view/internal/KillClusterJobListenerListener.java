/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.view.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import de.rcenvironment.core.gui.cluster.internal.ErrorMessageDialogFactory;
import de.rcenvironment.core.utils.cluster.ClusterJobInformation;

/**
 * Class responsible for killing cluster jobs.
 *
 * @author Doreen Seider
 */
public class KillClusterJobListenerListener implements SelectionListener {

    private static final String CANCELLING_JOBS_FAILED = "Cancelling jobs failed";
    
    private static final Log LOGGER = LogFactory.getLog(KillClusterJobListenerListener.class);
    
    private TableViewer tableViewer;
    
    private Action refreshAction;
    
    public KillClusterJobListenerListener(TableViewer tableViewer, Action refreshAction) {
        this.tableViewer = tableViewer;
        this.refreshAction = refreshAction;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        
        ISelection selection = tableViewer.getSelection();
        List<String> jobIds = new ArrayList<String>();
        if (selection != null && selection instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection) selection;
            for (@SuppressWarnings("unchecked")
                Iterator<ClusterJobInformation> iterator = sel.iterator(); iterator.hasNext();) {
                ClusterJobInformation informationEntry = iterator.next();
                jobIds.add(informationEntry.getJobId());
            }
        }
        try {
            ClusterJobInformationModel.getInstance().getClusterInformationService().cancelClusterJobs(jobIds);
        } catch (IOException e) {
            LOGGER.error(CANCELLING_JOBS_FAILED, e);
            ErrorMessageDialogFactory.createMessageDialogForCancelingJobsFailure(tableViewer.getTable(), e.getMessage()).open();
        }
        
        refreshAction.run();
    }

}

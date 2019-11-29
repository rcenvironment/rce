/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;

/**
 * Waits until all jobs of the job family {@link #MUST_BE_COMPLETED_ON_SHUTDOWN_JOB_FAMILY} completed.
 * 
 * @author Doreen Seider
 */
public final class UncompletedJobsShutdownListener implements IWorkbenchListener {

    /**
     * Constant defining the job family the jobs must belong to to be considered on shutdown.
     */
    public static final Object MUST_BE_COMPLETED_ON_SHUTDOWN_JOB_FAMILY = "de.rcenvironment.jobs";
    
    @Override
    public void postShutdown(final IWorkbench workbench) {
        IJobManager jobMan = Job.getJobManager();
        try {
            jobMan.join(MUST_BE_COMPLETED_ON_SHUTDOWN_JOB_FAMILY, null);
        } catch (InterruptedException e) {
            LogFactory.getLog(getClass()).error("Completing jobs failed", e);
        }
    }

    @Override
    public boolean preShutdown(IWorkbench workbench, boolean forced) {
        return true;
    }

}

/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * 
 * Activates the Bundle.
 * <ul>
 * <li>Registers an IWorkbenchListener to display a confirm dialog upon shutdown in case undisposed
 * workflows exist</li>
 * </ul>
 * 
 * @author Christian Weiss
 * @author Oliver Seebach
 */
public class Activator extends AbstractUIPlugin {

    /** Image identifier. */
    public static final String IMAGE_WORKFLOW_EDITOR_BACKGROUND = "de.rcenvironment.rce.gui.workflow.editor.background";

    /** Image identifier. */
    public static final String IMAGE_INPUT = "de.rcenvironment.rce.gui.workflow.editor.input";

    /** Image identifier. */
    public static final String IMAGE_OUTPUT = "de.rcenvironment.rce.gui.workflow.editor.output";

    /** Image identifier. */
    public static final String IMAGE_ADD_INPUT = "de.rcenvironment.rce.gui.workflow.editor.addinput";

    /** Image identifier. */
    public static final String IMAGE_ADD_OUTPUT = "de.rcenvironment.rce.gui.workflow.editor.addoutput";

    /** Image identifier. */
    public static final String IMAGE_RCE_ICON_16 = "de.rcenvironment.rce.icon.16";

    /** Image identifier. */
    public static final String IMAGE_RCE_ICON_32 = "de.rcenvironment.rce.icon.32";
    
    /** Image identifier. */
    public static final String IMAGE_COPY = "de.rcenvironment.rce.gui.copy";
    
    /** Image identifier. */
    public static final String UNKNOWN_STATE = "de.rcenvironment.rce.gui.workflow.state.unknown";

    private static Activator instance = null;

    private UncompletedJobsShutdownListener uncompletedJobsShutdownListener = null;
    
    private ActiveWorkflowShutdownListener undisposedWorkflowShutdownListener = null;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        instance = null;
    }
    
    /**
     * Registers {@link ActiveWorkflowShutdownListener} if not registered yet.
     */
    public synchronized void registerUndisposedWorkflowShutdownListener() {
        if (undisposedWorkflowShutdownListener == null) {
            undisposedWorkflowShutdownListener = new ActiveWorkflowShutdownListener();
            PlatformUI.getWorkbench().addWorkbenchListener(undisposedWorkflowShutdownListener);
        }
        if (uncompletedJobsShutdownListener == null) {
            uncompletedJobsShutdownListener = new UncompletedJobsShutdownListener();
            PlatformUI.getWorkbench().addWorkbenchListener(uncompletedJobsShutdownListener);
        }
    }
    
    /**
     * Unregisters {@link ActiveWorkflowShutdownListener} if registered.
     */
    public synchronized void unregisterUndisposedWorkflowShutdownListener() {
        if (undisposedWorkflowShutdownListener != null) {
            PlatformUI.getWorkbench().removeWorkbenchListener(undisposedWorkflowShutdownListener);
        }
        if (uncompletedJobsShutdownListener != null) {
            PlatformUI.getWorkbench().removeWorkbenchListener(uncompletedJobsShutdownListener);
        }
    }
    
    public static Activator getInstance() {
        return instance;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {

        Bundle bundle = Platform.getBundle("de.rcenvironment.core.gui.workflow");
        
        initializeImageRegistry1(reg, bundle);
        initializeImageRegistry2(reg, bundle);
        
        super.initializeImageRegistry(reg);
    }
    
    private void initializeImageRegistry1(ImageRegistry reg, Bundle bundle) {
        
        IPath path;
        URL url;
        ImageDescriptor desc;
        
        // idling
        path = new Path("resources/icons/states/ready.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.IDLING.name(), desc);
        
        // idling after reset
        path = new Path("resources/icons/states/resetting.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.RESETTING.name(), desc);
        
        // idling after reset
        path = new Path("resources/icons/states/reset.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.IDLING_AFTER_RESET.name(), desc);

        // preparing
        path = new Path("resources/icons/states/preparing.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.INIT.name(), desc);
        reg.put(WorkflowState.PREPARING.name(), desc);
        reg.put(ComponentState.INIT.name(), desc);
        reg.put(ComponentState.PREPARING.name(), desc);
        reg.put(ComponentState.PREPARED.name(), desc);

        // running
        path = new Path("resources/icons/states/run_enabled.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.RUNNING.name(), desc);
        reg.put(ComponentState.PROCESSING_INPUTS.name(), desc);
        reg.put(ComponentState.STARTING.name(), desc);
        
        // waiting for resources
        path = new Path("resources/icons/states/run_and_wait_for_resources.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.WAITING_FOR_RESOURCES.name(), desc);
        
        // waiting for verification
        path = new Path("resources/icons/states/waiting_for_approval.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.WAITING_FOR_APPROVAL.name(), desc);
        
        // verification failed
        path = new Path("resources/icons/states/results_rejected.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.RESULTS_REJECTED.name(), desc);
        reg.put(WorkflowState.RESULTS_REJECTED.name(), desc);
        
        // pausing
        path = new Path("resources/icons/states/suspend_disabled.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.PAUSING.name(), desc);
        reg.put(ComponentState.PAUSING.name(), desc);
        
        // paused
        path = new Path("resources/icons/states/suspend_enabled.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.PAUSED.name(), desc);
        reg.put(ComponentState.PAUSED.name(), desc);

        // resuming
        path = new Path("resources/icons/states/resume_enabled.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.RESUMING.name(), desc);
        reg.put(ComponentState.RESUMING.name(), desc);

        // tearing down
        path = new Path("resources/icons/states/tearing_down.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.TEARING_DOWN.name(), desc);
        
        // finished
        path = new Path("resources/icons/states/finished.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.FINISHED.name(), desc);
        reg.put(ComponentState.FINISHED.name(), desc);
    }
    
    private void initializeImageRegistry2(ImageRegistry reg, Bundle bundle) {
        
        IPath path;
        URL url;
        ImageDescriptor desc;
        
        // canceling
        path = new Path("resources/icons/states/cancel_disabled.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.CANCELING.name(), desc);
        reg.put(WorkflowState.CANCELING_AFTER_FAILED.name(), desc);
        reg.put(WorkflowState.CANCELING_AFTER_RESULTS_REJECTED.name(), desc);
        reg.put(ComponentState.CANCELLING.name(), desc);
        
        // canceled
        path = new Path("resources/icons/states/cancel_enabled.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.CANCELLED.name(), desc);
        reg.put(ComponentState.CANCELED.name(), desc);

        // failed
        path = new Path("resources/icons/states/failed.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.FAILED.name(), desc);
        reg.put(ComponentState.FAILED.name(), desc);

        // disposing
        path = new Path("resources/icons/states/disposed.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.DISPOSING.name(), desc);
        reg.put(WorkflowState.DISPOSED.name(), desc);
        reg.put(ComponentState.DISPOSING.name(), desc);
        reg.put(ComponentState.DISPOSED.name(), desc);

        // unknown
        path = new Path("/resources/icons/states/unknownState.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(WorkflowState.UNKNOWN.name(), desc);
        reg.put(ComponentState.UNKNOWN.name(), desc);
        
        // finished no run
        path = new Path("resources/icons/states/finishedNoRun.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(ComponentState.FINISHED_WITHOUT_EXECUTION.name(), desc);

        // editor background
        path = new Path("resources/editor-background.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(IMAGE_WORKFLOW_EDITOR_BACKGROUND, desc);

        // input icon
        path = new Path("resources/icons/endpoints/input.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(IMAGE_INPUT, desc);

        // output icon
        path = new Path("resources/icons/endpoints/output.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(IMAGE_OUTPUT, desc);

        // additional (dynamic) input icon
        path = new Path("resources/icons/endpoints/addInput.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(IMAGE_ADD_INPUT, desc);

        // additional (dynamic) output icon
        path = new Path("resources/icons/endpoints/addOutput.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(IMAGE_ADD_OUTPUT, desc);

        ImageManager sharedImageManager = ImageManager.getInstance();
        // RCE icons
        reg.put(IMAGE_RCE_ICON_16, sharedImageManager.getSharedImage(StandardImages.RCE_LOGO_16));
        reg.put(IMAGE_RCE_ICON_32, sharedImageManager.getSharedImage(StandardImages.RCE_LOGO_32));
        
        path = new Path("resources/icons/copy.gif");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        reg.put(IMAGE_COPY, desc);
    }
}

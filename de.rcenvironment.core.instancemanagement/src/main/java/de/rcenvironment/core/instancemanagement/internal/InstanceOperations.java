/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * 
 * Basic instance operations.
 *
 * @author David Scholz
 */
public interface InstanceOperations {

    /**
     * Name of the instanemanagement.lock file.
     */
    String INSTANCEMANAGEMENT_LOCK = "instancemanagement.lock";

    /**
     * Name of the shutdown.dat file.
     */
    String SHUTDOWN_FILE_NAME = "shutdown.dat";

    /**
     * Error message if acquiring lock file fails.
     */
    String UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON =
        "Unexpected error when trying to acquire a file lock on ";

    /**
     * 
     * Callbacklistener for command thread. Triggers callback if command is finished to do some clean up.
     *
     * @author David Scholz
     */
    public interface InstanceOperationCallbackListener {

        /**
         * 
         * Should be called on command finish.
         * 
         * @param profile the profile directory of a rce instance.
         * @throws IOException on failure.
         */
        void onCommandFinish(final File profile) throws IOException;
    }

    /**
     * Starts the given profile using the specified installation.
     * 
     * @param profileDirList the list of profile directories, as expected by the "--profile" parameter
     * @param installationDir the directory containing the installation (the main executable, /plugins, /configuration, ...)
     * @param timeout maximum time for the start up process.
     * @param userOutputReceiver the outputReceiver.
     * @param startWithGUI true if the instance shall be started with GUI
     * @throws InstanceOperationException on startup failure
     */
    void startInstanceUsingInstallation(final List<File> profileDirList, final File installationDir,
        final long timeout, TextOutputReceiver userOutputReceiver, boolean startWithGUI) throws InstanceOperationException;

    /**
     * Stops the instance using the given profile.
     * 
     * @param profileDirList the list of profile directories, as expected by the "--profile" parameter
     * @param timeout maximum time to shutdown instance.
     * @param userOutputReceiver the outputReceiver.
     * @throws InstanceOperationException on I/O exceptions while sending the shutdown signal
     */
    void shutdownInstance(List<File> profileDirList, final long timeout, TextOutputReceiver userOutputReceiver)
        throws InstanceOperationException;

    /**
     * Tests whether the given profile directory is locked by a running instance.
     * 
     * @param profileDir the profile directory, as expected by the "--profile" parameter
     * @return true if the directory is locked
     * @throws IOException on I/O exceptions while testing
     */
    boolean isProfileLocked(File profileDir) throws IOException;

    /**
     * 
     * Registers {@link InstanceOperationCallbackListener} to an implementation of {@link AbstractInstanceOperationsDecorator}.
     * 
     * @param callbackListener the listener to register.
     */
    void registerInstanceOperationCallbackListener(InstanceOperationCallbackListener callbackListener);

    /**
     * 
     * Unregisters all registered {@link InstanceOperationCallbackListener}.
     * 
     * @param callbackListener {@link InstanceOperationCallbackListener} to unregister.
     *
     */
    void unregisterInstanceOperationCallbackListener(InstanceOperationCallbackListener callbackListener);

    /**
     * 
     * Fires event to all registered {@link InstanceOperationCallbackListener}s.
     * 
     * @param profile the profile.
     * @throws IOException on failure.
     */
    void fireCommandFinishEvent(File profile) throws IOException;

}

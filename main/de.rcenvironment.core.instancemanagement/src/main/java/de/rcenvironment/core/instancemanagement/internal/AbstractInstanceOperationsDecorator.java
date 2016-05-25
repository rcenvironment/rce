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
 * Abstract basis class for {@link InstanceOperations} decorators.
 *
 * @author David Scholz
 */
// TODO review this decorator approach - misc_ro
public abstract class AbstractInstanceOperationsDecorator implements InstanceOperations {

    private final InstanceOperations delegate;

    public AbstractInstanceOperationsDecorator(InstanceOperations delegate) {
        this.delegate = delegate;
        if (delegate == null) {
            throw new IllegalStateException("No object to decorate is defined!");
        }
    }

    @Override
    public final boolean isProfileLocked(File profileDir) throws IOException {
        return delegate.isProfileLocked(profileDir);
    }

    @Override
    public final void shutdownInstance(List<File> profileDirList, long timeout, TextOutputReceiver userOutputReceiver)
        throws InstanceOperationException {
        try {
            beforeShutdown(profileDirList, timeout);
            delegate.shutdownInstance(profileDirList, timeout, userOutputReceiver);
        } catch (InstanceOperationException e) {
            onShutdownFailure(e.getFailedInstances());
            throw e;
        }
    }

    @Override
    public final void startInstanceUsingInstallation(List<File> profileDirList, File installationDir, long timeout,
        TextOutputReceiver userOutputReceiver, boolean startWithGUI)
        throws InstanceOperationException {
        try {
            beforeStart(profileDirList, installationDir, timeout);
            delegate.startInstanceUsingInstallation(profileDirList, installationDir, timeout, userOutputReceiver, startWithGUI);
        } catch (InstanceOperationException e) {
            onStartupFailure(e.getFailedInstances());
            throw e;
        }
    }

    abstract void beforeShutdown(List<File> profileDirList, long timeout) throws InstanceOperationException;

    abstract void beforeStart(List<File> profileDirList, File installationDir, long timeout) throws InstanceOperationException;

    abstract void onStartupFailure(List<File> profileDirList) throws InstanceOperationException;

    abstract void onShutdownFailure(List<File> profileDirList) throws InstanceOperationException;

    @Override
    public final void registerInstanceOperationCallbackListener(InstanceOperationCallbackListener callbackListener) {
        delegate.registerInstanceOperationCallbackListener(callbackListener);
    }

    @Override
    public final void unregisterInstanceOperationCallbackListener(InstanceOperationCallbackListener callbackListener) {
        delegate.unregisterInstanceOperationCallbackListener(callbackListener);
    }

    @Override
    public final void fireCommandFinishEvent(final File profile) throws IOException {
        delegate.fireCommandFinishEvent(profile);
    }

}

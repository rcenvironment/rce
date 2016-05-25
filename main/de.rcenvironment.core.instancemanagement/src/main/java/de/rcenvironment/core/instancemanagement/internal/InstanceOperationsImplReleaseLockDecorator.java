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
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * Decorator for {@link InstanceOperationsImpl#shutdownInstance(java.util.List, long)} and
 * {@link InstanceOperationsImpl#startInstanceUsingInstallation(java.util.List, java.io.File, long)} to centralise instancemanagment.lock
 * file release.
 *
 * @author David Scholz
 */
public final class InstanceOperationsImplReleaseLockDecorator extends AbstractInstanceOperationsDecorator {

    private final Log log = LogFactory.getLog(getClass());

    public InstanceOperationsImplReleaseLockDecorator(InstanceOperations delegate) {
        super(delegate);
        registerInstanceOperationCallbackListener(new InstanceOperationCallbackListener() {

            @Override
            public void onCommandFinish(File profile) throws IOException {
                try {
                    releaseAndDeleteLockFile(profile);
                } catch (IOException e) {
                    log.debug("Failed to release and delete instancemanagement.lock file.", e);
                    throw e;
                }
            }
        });
    }

    /**
     * 
     * Delegates to {@link InstanceOperationsImpl#startInstanceUsingInstallation(List, File, long)} and releases lock file after
     * {@link InstanceOperationCallbackListener#onCommandFinish(File)} is called.
     * 
     * @param profileDirList the profiles to start.
     * @param installationDir the installation directory of the instance installation.
     * @param timeout the maximum time this job is allowed to take.
     */
    @Override
    public void beforeStart(final List<File> profileDirList, final File installationDir, final long timeout) {
        // nothing to decorate
    }

    /**
     * 
     * Delegates to {@link InstanceOperationsImpl#shutdownInstance(List, long)} and releases lock file after
     * {@link InstanceOperationCallbackListener#onCommandFinish(File)} is called.
     * 
     * @param profileDirList the profiles to shutdown.
     * @param timeout the maximum time this job is allowed to take.
     */
    @Override
    public void beforeShutdown(final List<File> profileDirList, final long timeout) {
        // nothing to decorate
    }

    private void releaseIMLockFile(final File profile) throws IOException {
        File lockfile = new File(profile.getAbsolutePath() + "/" + INSTANCEMANAGEMENT_LOCK);
        FileLock lock = null;
        if (!lockfile.isFile()) {
            return;
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, "rw")) {
            synchronized (profile) {
                lock = randomAccessFile.getChannel().tryLock();
                if (lock != null) {
                    lock.release();
                } else {
                    throw new IOException("Could not release lock as it's hold by another instance.");
                }
            }       
        } catch (IOException e) {
            throw new IOException(UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON + lockfile, e);
        }
    }

    private void deleteIMFile(final File profile, final String name) throws IOException {
        if (profile.isDirectory()) {
            String fileName = "";
            if (name.equals("installation")) {
                fileName = name;
            } else {
                fileName = INSTANCEMANAGEMENT_LOCK;
            }
            for (File f : profile.listFiles()) {
                if (f.getName().equals(fileName)) {
                    log.info("try to delete " + fileName + " file of profile: " + profile.getName());
                    synchronized (profile) {
                        boolean success = f.delete();
                        if (!success) {
                            throw new IOException("Failed to delete " + fileName + " file.");
                        }
                        break;
                    }               
                }
            }
        }
    }

    private void releaseAndDeleteLockFile(final File profile) throws IOException {
        releaseIMLockFile(profile);
        deleteIMFile(profile, INSTANCEMANAGEMENT_LOCK);
    }

    @Override
    public void onStartupFailure(List<File> profileDirList) throws InstanceOperationException {
        for (File profile : profileDirList) {
            try {
                releaseAndDeleteLockFile(profile);
            } catch (IOException e1) {
                throw new InstanceOperationException(e1, null);
            }
        }
    }

    @Override
    public void onShutdownFailure(List<File> profileDirList) throws InstanceOperationException {
        for (File profile : profileDirList) {
            try {
                releaseAndDeleteLockFile(profile);
                deleteIMFile(profile, "installation");
            } catch (IOException e) {
                throw new InstanceOperationException(e, null);
            }            
        }
    }

}

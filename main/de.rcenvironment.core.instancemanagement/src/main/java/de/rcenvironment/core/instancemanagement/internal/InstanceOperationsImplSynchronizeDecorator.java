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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * Decorator for centralising synchronization of
 * {@link InstanceOperationsImpl#startInstanceUsingInstallation(java.util.List, java.io.File, long)} and
 * {@link InstanceOperationsImpl#shutdownInstance(java.util.List, long)}.
 *
 * @author David Scholz
 */
public final class InstanceOperationsImplSynchronizeDecorator extends AbstractInstanceOperationsDecorator {

    private Map<String, File> profileNameToProfileFileObjectMap = new ConcurrentHashMap<>();
    
    private Map<String, AtomicInteger> profileToCounterObjectMap = new ConcurrentHashMap<String, AtomicInteger>();

    public InstanceOperationsImplSynchronizeDecorator(InstanceOperations delegate) {
        super(delegate);
        registerInstanceOperationCallbackListener(new InstanceOperationCallbackListener() {

            @Override 
            public void onCommandFinish(File profile) throws IOException {
                synchronized (profileNameToProfileFileObjectMap) {
                    if (profileToCounterObjectMap.get(profile.getName()).getAndDecrement() == 0) {
                        profileNameToProfileFileObjectMap.remove(profile.getName());
                    }    
                }
            }
        });
    }

    /**
     * 
     * Delegates to {@link InstanceOperationsImplReleaseLockDecorator#startInstanceUsingInstallation(List, File, long)} and synchronizes
     * thread access with the profile file objects. They are deleted after {@link InstanceOperationCallbackListener#onCommandFinish(File)}
     * is called.
     * 
     * @param profileDirList profileDirList the profiles to start.
     * @param installationDir the installation directory of the instance installation.
     * @param timeout the maximum time this job is allowed to take.
     * @throws InstanceOperationException on failure.
     */
    @Override
    public void beforeStart(List<File> profileDirList, File installationDir, long timeout) throws InstanceOperationException {
        synchronized (profileNameToProfileFileObjectMap) {
            for (File profileDir : new ArrayList<>(profileDirList)) {
                if (profileNameToProfileFileObjectMap.containsKey(profileDir.getName())) {
                    profileDirList.remove(profileDir);
                    profileDirList.add(profileNameToProfileFileObjectMap.get(profileDir.getName()));
                    profileToCounterObjectMap.get(profileDir.getName()).incrementAndGet();
                } else {
                    profileNameToProfileFileObjectMap.put(profileDir.getName(), profileDir);
                    profileToCounterObjectMap.put(profileDir.getName(), new AtomicInteger(1));
                }
            }           
        }
    }

    /**
     * 
     * Delegates to {@link InstanceOperationsImplReleaseLockDecorator#startInstanceUsingInstallation(List, File, long)} and synchronizes
     * thread access with the profile file objects. They are destroyed after {@link InstanceOperationCallbackListener#onCommandFinish(File)}
     * is called.
     * 
     * @param profileDirList profileDirList the profiles to start.
     * @param timeout the maximum time this job is allowed to take.
     * @throws InstanceOperationException on failure
     */
    @Override
    public void beforeShutdown(List<File> profileDirList, long timeout) throws InstanceOperationException {
        synchronized (profileNameToProfileFileObjectMap) {
            for (File profileDir : new ArrayList<>(profileDirList)) {
                if (profileNameToProfileFileObjectMap.containsKey(profileDir.getName())) {
                    profileDirList.remove(profileDir);
                    profileDirList.add(profileNameToProfileFileObjectMap.get(profileDir.getName()));
                    profileToCounterObjectMap.get(profileDir.getName()).decrementAndGet();
                } else {
                    profileNameToProfileFileObjectMap.put(profileDir.getName(), profileDir);
                    profileNameToProfileFileObjectMap.put(profileDir.getName(), profileDir);
                    profileToCounterObjectMap.put(profileDir.getName(), new AtomicInteger(1));
                }
            }
        }
    }

    @Override
    public void onStartupFailure(List<File> profileDirList) {
        synchronized (profileNameToProfileFileObjectMap) {
            for (File failedInstance : profileDirList) {
                profileNameToProfileFileObjectMap.remove(failedInstance.getName());
            }
        }
    }

    @Override
    public void onShutdownFailure(List<File> profileDirList) {
        synchronized (profileNameToProfileFileObjectMap) {
            for (File failedInstance : profileDirList) {
                profileNameToProfileFileObjectMap.remove(failedInstance.getName());
            }
        }
    }

}

/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import de.rcenvironment.core.shutdown.HeadlessShutdown;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * 
 * Task, which asynchronously terminates a rce instance.
 *
 * @author David Scholz
 */
public class InstanceShutdownTask implements Runnable {

    private static final String FAILED_TO_SHUTDOWN_MESSAGE = "Failed to shutdown instance with id %s. Aborted with message:  %s";

    private final File profile;

    private final long timeout;

    private final TextOutputReceiver userOutputReceiver;

    private final CountDownLatch globalLatch;

    private final CountDownLatch indicatesShutdownSuccess;

    public InstanceShutdownTask(File profile, long timeout, TextOutputReceiver userOutputReceiver, CountDownLatch globalLatch,
        CountDownLatch indicatesShutdownSuccess) {
        this.profile = profile;
        this.timeout = timeout;
        this.userOutputReceiver = userOutputReceiver;
        this.globalLatch = globalLatch;
        this.indicatesShutdownSuccess = indicatesShutdownSuccess;
    }

    @Override
    @TaskDescription("Instance Management: Shutting down an instance")
    public void run() {

        try {
            InstanceOperationsUtils.lockIMLockFile(profile, timeout);
        } catch (IOException e) {
            userOutputReceiver.addOutput(StringUtils.format(InstanceOperationsUtils.TIMEOUT_REACHED_MESSAGE, profile.getName()));
            releaseLockIfErrorOccurs();
            return;
        }

        try {
            if (!InstanceOperationsUtils.isProfileLocked(profile)) {
                releaseLockIfErrorOccurs();
                return;
            }
        } catch (IOException e) {
            userOutputReceiver.addOutput(
                "Couldn't check if profile with id : " + profile.getName() + " is running. Aborted with message: " + e.getMessage());
            releaseLockIfErrorOccurs();
            return;
        }

        final int maxWaitIterations = 50;
        final int singleWaitIteration = 500;

        try {
            if (!InstanceOperationsUtils.detectShutdownFile(profile.getAbsolutePath())) {
                releaseLockIfErrorOccurs();
            }
        } catch (IOException e) {
            userOutputReceiver.addOutput("Unexpected failure while detecting shutdown file.");
            releaseLockIfErrorOccurs();
            return;
        }

        try {
            userOutputReceiver.addOutput("Trying to shutdown instance with id: " + profile.getName());
            new HeadlessShutdown().shutdownExternalInstance(profile);
        } catch (IOException e) {
            userOutputReceiver
                .addOutput(StringUtils.format(FAILED_TO_SHUTDOWN_MESSAGE, profile.getName(), e.getMessage()));
            releaseLockIfErrorOccurs();
            return;
        }

        // after sending the signal wait for the instance JVM to terminate, which releases the lock
        for (int i = 0; i < maxWaitIterations; i++) {

            try {
                if (!InstanceOperationsUtils.isProfileLocked(profile)) {

                    // success --> delete instance.lock
                    InstanceOperationsUtils.deleteInstanceLockFromProfileFolder(profile);
                    globalLatch.countDown();
                    indicatesShutdownSuccess.countDown();
                    userOutputReceiver.addOutput("Successfully shutdown instance with id: " + profile.getName());
                    return;
                } else {
                    try {
                        Thread.sleep(singleWaitIteration);
                    } catch (InterruptedException e) {
                        userOutputReceiver
                            .addOutput(
                                StringUtils.format(FAILED_TO_SHUTDOWN_MESSAGE, profile.getName(), e.getMessage()));
                        releaseLockIfErrorOccurs();
                        return;
                    }
                }
            } catch (IOException e) {
                userOutputReceiver.addOutput("Failed to check profile lock of instance with id: " + profile.getName() + ".");
                releaseLockIfErrorOccurs();
                return;
            }
        }

        releaseLockIfErrorOccurs();
        userOutputReceiver
            .addOutput(
                StringUtils.format(FAILED_TO_SHUTDOWN_MESSAGE, profile.getName(), "unexpected failure."));
        return;
    }

    private void releaseLockIfErrorOccurs() {
        indicatesShutdownSuccess.countDown();
        globalLatch.countDown();
    }

}

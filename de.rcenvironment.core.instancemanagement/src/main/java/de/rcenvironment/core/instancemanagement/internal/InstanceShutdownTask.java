/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

    private static final String SINGLE_QUOTE = "'";

    private static final String FAILED_TO_SHUTDOWN_MESSAGE = "Failed to shut down instance '%s' - aborted with message '%s'";

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
                "Couldn't check if instance '" + profile.getName() + "' is running - aborted with message '" + e.getMessage()
                    + SINGLE_QUOTE);
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
            userOutputReceiver.addOutput("Trying to shutdown instance '" + profile.getName() + SINGLE_QUOTE);
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
                    userOutputReceiver.addOutput("Successfully shut down instance '" + profile.getName() + SINGLE_QUOTE);
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
                userOutputReceiver.addOutput("Failed to check profile lock of instance '" + profile.getName() + SINGLE_QUOTE);
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

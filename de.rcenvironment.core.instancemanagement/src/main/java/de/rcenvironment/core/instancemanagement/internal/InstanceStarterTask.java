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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.instancemanagement.InstanceStatus;
import de.rcenvironment.core.instancemanagement.InstanceStatus.InstanceState;
import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.AbstractTextOutputReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * 
 * Tasks, which starts an RCE instance via a {@link LocalApacheCommandLineExecutor}.
 *
 * @author David Scholz
 * @author Robert Mischke
 * @author Lukas Rosenbach
 */
public class InstanceStarterTask implements Runnable {

    private static final String START_UP_HEADLESS_MESSAGE = "Starting instance %s in headless mode...";

    private static final String START_UP_GUI_MESSAGE = "Starting instance %s in GUI mode...";

    private final long timeout;

    private final boolean startWithGUI;

    private final TextOutputReceiver userOutputReceiver;

    private final File profile;

    private final File installationDir;

    private final Log log = LogFactory.getLog(getClass());

    private final CountDownLatch globalLatch;

    private final CountDownLatch startupOutputDetected;

    private final AtomicBoolean startupOutputIndicatesSuccess;
    
    private final InstanceStatus status;
    
    private final String parameters;

    public InstanceStarterTask(long timeout, boolean startWithGUI, TextOutputReceiver userOutputReceiver, File profile,
        File installationDir, String parameters, CountDownLatch globalLatch, CountDownLatch startuputOutputDetected,
        AtomicBoolean startupOutputIndicatesSucess, InstanceStatus status) {
        this.timeout = timeout;
        this.startWithGUI = startWithGUI;
        this.userOutputReceiver = userOutputReceiver;
        this.profile = profile;
        this.installationDir = installationDir;
        this.globalLatch = globalLatch;
        this.startupOutputDetected = startuputOutputDetected;
        this.startupOutputIndicatesSuccess = startupOutputIndicatesSucess;
        this.status = status;
        if (parameters == null) {
            this.parameters = "";
        } else {
            this.parameters = parameters;
        }  
    }

    @Override
    @TaskDescription("Instance Management: Starting an instance")
    public void run() {

        try {

            InstanceOperationsUtils.lockIMLockFile(profile, timeout);

        } catch (IOException e) {

            userOutputReceiver
                .addOutput(StringUtils.format(InstanceOperationsUtils.TIMEOUT_REACHED_MESSAGE, profile.getName()));
            releaseLockIfErrorOccurs();
            return;
        }

        // if there is a .metadata/.log file in the default workspace, delete it
        final Path metadataLogFile = new File(profile, "workspace/.metadata/.log").toPath();
        try {
            Files.deleteIfExists(metadataLogFile);
        } catch (IOException e) {
            userOutputReceiver.addOutput("Failed to delete the existing .metadata/.log file " + metadataLogFile
                + " before starting the containing profile; error: " + e.toString());
            releaseLockIfErrorOccurs();
            return;
        }

        LocalApacheCommandLineExecutor executor;

        try {

            executor = new LocalApacheCommandLineExecutor(installationDir);

        } catch (IOException e) {

            userOutputReceiver
                .addOutput("The installation directory " + installationDir.getAbsolutePath()
                    + " does not exist or can't be created. Aborting startup of instance with id: " + profile.getName() + ".");
            releaseLockIfErrorOccurs();
            return;
        }

        try {

            if (OSFamily.isWindows()) {

                if (!startWithGUI) {
                    // note: using "-p" because "--profile" was not available in 6.0.x
                    userOutputReceiver.addOutput(StringUtils.format(START_UP_HEADLESS_MESSAGE, profile.getName()));
                    executor.start(StringUtils.format("rce --headless -nosplash -p \"%s\" %s", profile.getAbsolutePath(), parameters));
                } else {
                    userOutputReceiver.addOutput(StringUtils.format(START_UP_GUI_MESSAGE, profile.getName()));
                    executor.start(StringUtils.format("rce -p \"%s\" --use-default-workspace %s", profile.getAbsolutePath(), parameters));
                }

            } else {

                if (!startWithGUI) {
                    // note: using "-p" because "--profile" was not available in 6.0.x
                    userOutputReceiver.addOutput(StringUtils.format(START_UP_HEADLESS_MESSAGE, profile.getName()));
                    executor.start(StringUtils.format("./rce --headless -nosplash -p \"%s\" %s", profile.getAbsolutePath(), parameters));
                } else {
                    userOutputReceiver.addOutput(StringUtils.format(START_UP_GUI_MESSAGE, profile.getName()));
                    executor.start(StringUtils.format("./rce -p \"%s\" --use-default-workspace %s", profile.getAbsolutePath(), parameters));
                }

            }

        } catch (IOException e) {

            userOutputReceiver.addOutput("An error occured on the target executable of instance with id: " + profile.getName()
                + ". Process aborted with message: " + e.getMessage());
            // TODO what to do with the locks here?
            // releaseLockIfErrorOccurs();
            return;
        }

        startStdOutStreamWatcher(executor, profile.getName());
        startStdErrStreamWatcher(executor, profile.getName());
        
        try {

            executor.waitForTermination();

            // TODO add useroutput
        } catch (IOException e) {
            executor.cancel();
            releaseLockIfErrorOccurs();
        } catch (InterruptedException e) {
            executor.cancel();
            releaseLockIfErrorOccurs();
        }

    }

    private void releaseLockIfErrorOccurs() {
        startupOutputDetected.countDown();
    }

    private void startStdOutStreamWatcher(final LocalApacheCommandLineExecutor executor, final String profileName) {

        TextStreamWatcherFactory.create(executor.getStdout(), new AbstractTextOutputReceiver() {

            @Override
            public void addOutput(String line) {
                log.debug("Stdout of instance '" + profileName + "': " + line);
                if (line.contains("startup complete")) {
                    startupOutputDetected.countDown();
                    globalLatch.countDown();
                    userOutputReceiver.addOutput("Successfully started instance " + profileName);
                    status.setInstanceState(InstanceState.RUNNING);
                }
            }

        }).start();
    }

    private void startStdErrStreamWatcher(final LocalApacheCommandLineExecutor executor, final String profileName) {

        TextStreamWatcherFactory.create(executor.getStderr(), new AbstractTextOutputReceiver() {

            @Override
            public void addOutput(String line) {
                log.debug("Stderr of instance '" + profileName + "': " + line);
                // TODO check whether this actually overrides the success check above
                if (line.contains("Failed to lock profile with id " + profileName)) {
                    // TODO capture fallback location? (and maybe shut it down immediately?)
                    startupOutputIndicatesSuccess.compareAndSet(true, false);
                    startupOutputDetected.countDown();
                    globalLatch.countDown();
                }
            }

        }).start();

    }

}

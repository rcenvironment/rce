/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.wrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.datamanagement.stateful.StatefulComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.AbstractTextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.LoggingTextOutReceiver;
import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;

/*
 * Copyright (C) 2006-2011 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

/**
 * An abstract base class for wrappers of command-line tools.
 * 
 * @author Robert Mischke
 * 
 * @param <C> the type of the configuration object passed to each tool invocation
 * @param <R> the type of the result returned from each tool invocation
 */
public abstract class WrapperBase<C, R> {

    /**
     * A simple {@link TextOutputReceiver} adapter that forwards all captured text lines to the
     * "stdout" monitoring channel.
     * 
     * @author Robert Mischke
     * 
     */
    protected final class StdoutMonitoringForwarder extends AbstractTextOutputReceiver {

        @Override
        public void addOutput(String line) {
            monitoringListener.appendStdout(line);
        }
    }

    /**
     * A simple {@link TextOutputReceiver} adapter that forwards all captured text lines to the
     * "stderr" monitoring channel.
     * 
     * @author Robert Mischke
     */
    protected final class StderrMonitoringForwarder extends AbstractTextOutputReceiver {

        @Override
        public void addOutput(String line) {
            monitoringListener.appendStderr(line);
        }
    }

    protected final MonitoringEventListener monitoringListener;

    protected final StdoutMonitoringForwarder stdoutMonitoringForwarder;

    protected final StderrMonitoringForwarder stderrMonitoringForwarder;

    protected final Log log = LogFactory.getLog(getClass());

    private final List<BasicWrapperHook> registeredHooks;

    private final StatefulComponentDataManagementService fileReferenceHandler;

    private ComponentContext componentContext;

    protected WrapperBase(StatefulComponentDataManagementService fileReferenceHandler, MonitoringEventListener listener,
        ComponentContext compInformation) {
        this.monitoringListener = listener;
        this.fileReferenceHandler = fileReferenceHandler;
        this.stdoutMonitoringForwarder = new StdoutMonitoringForwarder();
        this.stderrMonitoringForwarder = new StderrMonitoringForwarder();
        this.registeredHooks = new ArrayList<BasicWrapperHook>();
        this.componentContext = compInformation;
    }

    /**
     * Adds a new lifecycle hook/listener.
     * 
     * @param hook the hook to add
     */
    public void registerHook(BasicWrapperHook hook) {
        registeredHooks.add(hook);
    }

    /**
     * Removes a lifecycle hook/listener.
     * 
     * @param hook the hook to remove
     */
    public void unregisterHook(BasicWrapperHook hook) {
        registeredHooks.remove(hook);
    }

    /**
     * Performs initialization steps that are only required once for repeated execution runs, for
     * example network connections to the execution host.
     * 
     * @throws IOException on I/O errors
     * @throws ValidationFailureException on invalid configuration values
     */
    public void setupStaticEnvironment() throws IOException, ValidationFailureException {
        // empty by default; subclasses may (but don't have to) override
    }

    /**
     * The counterpart to {@link #setupStaticEnvironment()}; responsible for cleaning up everything
     * that is not meant to remain on the execution host.
     * 
     * @throws IOException on I/O errors
     */
    public void tearDownStaticEnvironment() throws IOException {
        // empty by default; subclasses may (but don't have to) override
    }

    /**
     * Performs a single tool invocation.
     * 
     * @param runConfiguration the subclass-specific configuration object
     * @return the subclass-specific result object
     * @throws Exception on errors; TODO narrow down; does ComponentException fit here?
     */
    public abstract R execute(C runConfiguration) throws Exception;

    /**
     * Convenience variant of {@link #execute(Object)} that sets a hook only for the duration of
     * this single execution.
     * 
     * @param runConfiguration the subclass-specific configuration object
     * @param singleRunHook the hook to register only for this single execution
     * @return the subclass-specific result object
     * @throws Exception on errors; TODO narrow down; does ComponentException fit here?
     */
    public R execute(C runConfiguration, BasicWrapperHook singleRunHook) throws Exception {
        registerHook(singleRunHook);
        try {
            return execute(runConfiguration);
        } finally {
            unregisterHook(singleRunHook);
        }
    }

    /**
     * Uploads a set of files to the work directory. In the given map, the keys are the
     * workdir-relative filenames, and the values are the abstract content references.
     * 
     * @param fileMap the filename-to-reference map
     * @param executor the executor to use
     * @throws IOException on I/O errors
     */
    protected void uploadInputFiles(Map<String, String> fileMap, CommandLineExecutor executor) throws IOException {
        for (Entry<String, String> entry : fileMap.entrySet()) {
            String filename = entry.getKey();
            String reference = entry.getValue();
            uploadFileFromReference(filename, reference, executor);
        }
    }

    /**
     * @param filename the workdir-relative filename to create the remote file as
     * @param reference the abstract content reference
     * @param executor the command executor to use
     * @throws IOException on I/O errors
     */
    private void uploadFileFromReference(String filename, String reference, CommandLineExecutor executor) throws IOException {
        // TODO could be optimized with streaming pipe-through from DM to target
        File tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("upload." + filename + "-*.tmp");
        tempFile.deleteOnExit(); // safeguard
        try {
            getDataManagementService().copyReferenceToLocalFile(reference, tempFile, componentContext.getDefaultStorageNodeId());

            // monitoringListener.appendUserInformation("Uploading " + filename);
            executor.uploadFileToWorkdir(tempFile, filename);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    /**
     * @param filename the workdir-relative filename to copy the remote file from
     * @param reference the abstract content reference
     * @param executor the command executor to use
     * @throws IOException on I/O errors
     */
    protected String downloadFileToReference(String filename, CommandLineExecutor executor) throws IOException {
        // TODO optimize by adding stream methods
        File targetFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("download." + filename + "-*.tmp");
        targetFile.deleteOnExit(); // safeguard
        try {
            executor.downloadFileFromWorkdir(filename, targetFile);
            return getDataManagementService().createTaggedReferenceFromLocalFile(targetFile, filename);
        } finally {
            FileUtils.deleteQuietly(targetFile);
        }
    }

    protected void prepareAndTestEnvironment(CommandLineExecutor executor) throws InterruptedException, IOException {

        // FIXME this was originally a setup method; implement actual testing of expected state

        log.debug("Environment pre-test starting");

        executor.start("echo \"Effective Workdir: `pwd`\"", null);

        InputStream ansysStdout = executor.getStdout();
        InputStream ansysStderr = executor.getStderr();

        final TextStreamWatcher stdoutWatcher =
            new TextStreamWatcher(ansysStdout, new LoggingTextOutReceiver("Environment pre-test Stdout"));
        final TextStreamWatcher stderrWatcher =
            new TextStreamWatcher(ansysStderr, new LoggingTextOutReceiver("Environment pre-test Stderr"));

        stdoutWatcher.start();
        stderrWatcher.start();

        executor.waitForTermination();

        stdoutWatcher.waitForTermination();
        stderrWatcher.waitForTermination();

        log.debug("Environment pre-test complete");
    }

    protected StatefulComponentDataManagementService getDataManagementService() {
        return fileReferenceHandler;
    }

}

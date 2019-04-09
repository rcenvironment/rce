/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.cluster.execution;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.components.cluster.execution.internal.ClusterJobFinishListener;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystemConstants;
import de.rcenvironment.core.utils.cluster.ClusterService;
import de.rcenvironment.core.utils.cluster.ClusterServiceManager;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfigurationFactory;
import de.rcenvironment.core.utils.ssh.jsch.executor.context.JSchExecutorContext;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Submits a job to a cluster batch system with upload and download of directories.
 * 
 * Some general notes:
 * 
 * -The cluster component is able to submit multiple jobs at once (within one component run). If I'd design it anew nowadays, I'd cut this
 * feature and would only support one job submission per component run. From my point of view, the cluster component functionality was not
 * well designed in the first place due to some misunderstandings on the use case side
 * 
 * - The "multiple job" feature results in an endpoint interface that is complicated. If I'd design it anew nowadays, I'd go for one input
 * directory and one output directory and would get rid of the job count indicating how many input directories are expected
 * 
 * - The code and especially the exception handling has some flaws, I'd do it nowadays a bit different, more robust, less RuntimeExceptions.
 * So, it might be worth to refactor the code a bit to keep it maintainable on the long run.
 * 
 * -- seid_do
 * 
 * @author Doreen Seider
 */
public class ClusterComponent extends DefaultComponent {

    private static final String FAILED_TO_WAIT_FOR_JOB_TO_BECOME_COMPLETED = "Failed to wait for job to become completed";

    private static final String FAILED_TO_SUBMIT_JOB = "Failed to submit job: ";

    private static final String FAILED_FILE_NAME = "cluster_job_failed";

    private static final String SLASH = "/";

    private static final String OUTPUT_FOLDER_NAME = "output";

    private static final String AT = "@";

    private static final String PATH_PATTERN = "iteration-%d/cluster-job-%d";

    private static Log log = LogFactory.getLog(ClusterComponent.class);

    private final Object executorLock = new Object();

    private ComponentLog componentLog;

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private SshSessionConfiguration sshConfiguration;

    private JSchExecutorContext context;

    private CommandLineExecutor executor;

    private ClusterService clusterService;

    private ClusterQueuingSystem queuingSystem;

    private Map<String, String> pathsToQueuingSystemCommands;

    private List<String> jobIds = Collections.synchronizedList(new ArrayList<String>());

    private AtomicReference<CountDownLatch> jobsCountDefinedLatch = new AtomicReference<>(null);

    private AtomicReference<CountDownLatch> jobsSubmittedLatch = new AtomicReference<>(null);

    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    private Integer jobCount = null;

    private boolean considerSharedInputDir = true;

    private int iteration = 0;

    private Semaphore upDownloadSemaphore;

    private boolean isJobScriptProvidedWithinInputDir;

    private Map<String, Deque<TypedDatum>> inputValues = new HashMap<>();

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        componentLog = componentContext.getLog();
    }

    @Override
    public void start() throws ComponentException {
        ClusterServiceManager clusterServiceManager = componentContext.getService(ClusterServiceManager.class);
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);

        ConfigurationService configurationService = componentContext.getService(ConfigurationService.class);
        // TODO 6.0.0 review: preliminary path
        ClusterComponentConfiguration clusterConfiguration = new ClusterComponentConfiguration(
            configurationService.getConfigurationSegment("componentSettings/de.rcenvironment.cluster"));

        isJobScriptProvidedWithinInputDir = Boolean.valueOf(componentContext.getConfigurationValue(
            ClusterComponentConstants.KEY_IS_SCRIPT_PROVIDED_WITHIN_INPUT_DIR));

        String host = componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_HOST);
        Integer port = Integer.valueOf(componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_PORT));
        String authUser = componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_AUTH_USER);
        String authPhrase = componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_AUTH_PHRASE);
        String sandboRootWorkDir = componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SANDBOXROOT);

        queuingSystem = ClusterQueuingSystem.valueOf(componentContext.getConfigurationValue(
            ClusterComponentConstants.CONFIG_KEY_QUEUINGSYSTEM));
        pathsToQueuingSystemCommands = ClusterComponentConstants.extractPathsToQueuingSystemCommands(
            componentContext.getConfigurationValue(ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS));
        sshConfiguration = SshSessionConfigurationFactory.createSshSessionConfigurationWithAuthPhrase(host, port, authUser, authPhrase);

        clusterService = clusterServiceManager.retrieveSshBasedClusterService(
            queuingSystem,
            pathsToQueuingSystemCommands,
            sshConfiguration.getDestinationHost(),
            sshConfiguration.getPort(),
            sshConfiguration.getSshAuthUser(),
            sshConfiguration.getSshAuthPhrase());

        context = new JSchExecutorContext(sshConfiguration, sandboRootWorkDir);

        try {
            context.setUpSession();
            componentLog.componentInfo("Session established: " + authUser + AT + host + ":" + port);
        } catch (IOException e) {
            throw new ComponentException("Failed to establish connection to remote host", e);
        } catch (ValidationFailureException e) {
            throw new ComponentException("Failed to validate passed parameters", e);
        }

        try {
            executor = context.setUpSandboxedExecutor();
            componentLog.componentInfo("Remote sandbox created: " + executor.getWorkDirPath());
        } catch (IOException e) {
            throw new ComponentException("Failed to set up remote sandbox", e);
        }

        upDownloadSemaphore = new Semaphore(clusterConfiguration.getMaxChannels());

        if (!componentContext.getInputs().contains(ClusterComponentConstants.INPUT_JOBCOUNT)) {
            jobCount = 1;
        }
        if (!componentContext.getInputs().contains(ClusterComponentConstants.INPUT_SHAREDJOBINPUT)) {
            considerSharedInputDir = false;
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        jobsCountDefinedLatch.set(new CountDownLatch(1));
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (!inputValues.containsKey(inputName)) {
                inputValues.put(inputName, new LinkedList<TypedDatum>());
            }
            inputValues.get(inputName).add(componentContext.readInput(inputName));
        }
        if (jobCount == null && inputValues.containsKey(ClusterComponentConstants.INPUT_JOBCOUNT)) {
            jobCount = readAndEvaluateJobCount();
        }
        if (jobCount != null && inputValues.containsKey(ClusterComponentConstants.INPUT_JOBINPUTS)
            && inputValues.get(ClusterComponentConstants.INPUT_JOBINPUTS).size() >= jobCount
            && (!considerSharedInputDir || (inputValues.containsKey(ClusterComponentConstants.INPUT_SHAREDJOBINPUT)
                && inputValues.get(ClusterComponentConstants.INPUT_SHAREDJOBINPUT).size() >= 1))) {
            jobsSubmittedLatch.set(new CountDownLatch(jobCount));
            jobsCountDefinedLatch.get().countDown();
            // consume inputs
            List<DirectoryReferenceTD> inputDirs = new ArrayList<>();
            for (int i = 0; i < jobCount; i++) {
                inputDirs.add((DirectoryReferenceTD) inputValues.get(ClusterComponentConstants.INPUT_JOBINPUTS).poll());
            }
            DirectoryReferenceTD sharedInputDir = null;
            if (considerSharedInputDir) {
                sharedInputDir = (DirectoryReferenceTD) inputValues
                    .get(ClusterComponentConstants.INPUT_SHAREDJOBINPUT).poll();
            }

            // upload
            uploadInputDirectories(inputDirs, sharedInputDir);

            if (!isJobScriptProvidedWithinInputDir) {
                uploadJobScript();
            }

            // execution
            Queue<BlockingQueue<String>> queues = submitJobs();

            // download
            downloadDirectoriesAndSendToOutputsOnJobFinished(queues);

            jobCount = null;
            iteration++;
        }
    }

    @Override
    public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
        isCancelled.set(true);
        try {
            jobsCountDefinedLatch.get().await();
            jobsSubmittedLatch.get().await();
            String stdErr = clusterService.cancelClusterJobs(jobIds);
            if (!stdErr.isEmpty()) {
                componentLog.componentError(stdErr);
            }
        } catch (InterruptedException e) {
            componentLog.componentError("Interrupted while cancelling cluster job(s)");
        } catch (IOException e) {
            componentLog.componentError("Failed to cancel cluster job(s): " + e.getMessage());
        }
    }

    private Integer readAndEvaluateJobCount() throws ComponentException {
        Integer count = Integer.valueOf((int) ((IntegerTD) inputValues.get(ClusterComponentConstants.INPUT_JOBCOUNT).poll()).getIntValue());
        if (count <= 0) {
            throw new ComponentException(StringUtils.format("Job count is invalid. It is %d, but must be greater than 0", count));
        }
        return count;
    }

    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        deleteSandboxIfNeeded();
    }

    private void deleteSandboxIfNeeded() {
        Boolean deleteSandbox = Boolean.valueOf(componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_DELETESANDBOX));

        if (executor != null && deleteSandbox) {
            String sandbox = executor.getWorkDirPath();
            try {
                // delete here explicitly as context.tearDownSandbox(executor) doesn't support -r option for safety reasons
                executor.start("rm -r " + sandbox);
                context.tearDownSession();
                componentLog.componentInfo("Remote sandbox deleted: " + sandbox);
            } catch (IOException e) {
                String errorMessage = "Failed to delete remote sandbox '%s'";
                componentLog.componentInfo(StringUtils.format(errorMessage, sandbox) + ": " + e.getMessage());
                log.error(StringUtils.format(errorMessage, sandbox), e);
            }
        }
    }

    private void uploadJobScript() throws ComponentException {
        String message = "Failed to upload job script";
        try {
            File jobFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                ClusterComponentConstants.JOB_SCRIPT_NAME);
            // replace Windows newline as TORQUE complains if Windows text format
            FileUtils.write(jobFile,
                componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT).replaceAll("\r\n", "\n"));
            componentLog.componentInfo("Uploading job script: " + jobFile.getName());
            upDownloadSemaphore.acquire();
            executor.uploadFileToWorkdir(jobFile, ".");
            upDownloadSemaphore.release();
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(jobFile);
            componentLog.componentInfo("Job script uploaded: " + jobFile.getName());
        } catch (IOException | InterruptedException e) {
            throw new ComponentException(message, e);
        }

    }

    private void uploadInputDirectories(List<DirectoryReferenceTD> inputDirs, final DirectoryReferenceTD sharedInputDir) {

        componentLog.componentInfo("Uploading input directories...");
        int count = 0;
        CallablesGroup<RuntimeException> callablesGroup =
            ConcurrencyUtils.getFactory().createCallablesGroup(RuntimeException.class);

        for (final DirectoryReferenceTD inputDir : inputDirs) {
            final int countSnapshot = count++;
            callablesGroup.add(new Callable<RuntimeException>() {

                @Override
                @TaskDescription("Upload input directory for cluster job execution")
                public RuntimeException call() throws Exception {
                    try {
                        uploadInputDirectory(inputDir, "/cluster-job-" + countSnapshot, "input");
                        return null;
                    } catch (RuntimeException e) {
                        return e;
                    }
                }
            });
        }

        if (sharedInputDir != null) {
            callablesGroup.add(new Callable<RuntimeException>() {

                @Override
                @TaskDescription("Upload shared input directory for cluster job execution")
                public RuntimeException call() throws Exception {
                    try {
                        componentLog.componentInfo("Uploading shared input directory...");
                        uploadInputDirectory(sharedInputDir, "", "cluster-job-shared-input");
                        componentLog.componentInfo("Shared input directory uploaded");
                        return null;
                    } catch (RuntimeException e) {
                        return e;
                    }
                }
            });
        }

        List<RuntimeException> exceptions = callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                // should never happen
                log.warn("Illegal state: Uncaught exception from Callable", e);
            }
        });

        for (RuntimeException e : exceptions) {
            if (e != null) {
                log.error("Exception caught when uploading directories", e);
            }
        }

        for (RuntimeException e : exceptions) {
            if (e != null) {
                throw e;
            }
        }
        componentLog.componentInfo("Input directories uploaded");
    }

    private void uploadInputDirectory(DirectoryReferenceTD jobDir, String directoryParent, String dirName) throws ComponentException {
        String message = "Failed to upload directory: ";
        try {
            File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
            dataManagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext, jobDir, dir);
            componentLog.componentInfo("Uploading directory: " + jobDir.getDirectoryName());
            File inputDir = new File(dir, dirName);
            File origDir = new File(dir, jobDir.getDirectoryName());
            if (!origDir.renameTo(inputDir)) {
                throw new IOException(StringUtils.format("Failed to rename directory for an unknown reason: %s->%s", origDir, inputDir));
            }
            upDownloadSemaphore.acquire();
            executor.uploadDirectoryToWorkdir(inputDir, "iteration-" + iteration + directoryParent);
            upDownloadSemaphore.release();
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
            componentLog.componentInfo("Directory uploaded: " + jobDir.getDirectoryName());
        } catch (IOException | InterruptedException e) {
            throw new ComponentException(message + jobDir.getDirectoryName(), e);
        }
    }

    private Queue<BlockingQueue<String>> submitJobs() throws ComponentException {
        Queue<BlockingQueue<String>> blockingQueues = new LinkedList<>();
        for (int i = 0; i < jobCount; i++) {
            blockingQueues.add(submitJob(i));
        }

        return blockingQueues;
    }

    private BlockingQueue<String> submitJob(int job) throws ComponentException {

        final int byteBuffer = 10000;

        String stdout;

        try {
            String mkDirOutputCommand = "mkdir " + getOutputFolderPath(job) + " ";
            executor.start(mkDirOutputCommand);
            executor.waitForTermination();
            String qsubCommand = buildQsubCommand(getJobFolderPath(job));
            executor.start(qsubCommand);
            componentLog.componentInfo(StringUtils.format("Job submitted: %s from %s",
                ClusterComponentConstants.JOB_SCRIPT_NAME, getJobFolderPath(job)));

            try (InputStream stdoutStream = executor.getStdout();
                InputStream stderrStream = executor.getStderr()) {

                executor.waitForTermination();

                try (BufferedInputStream bufferedStdoutStream = new BufferedInputStream(stdoutStream);
                    BufferedInputStream bufferedStderrStream = new BufferedInputStream(stderrStream)) {

                    bufferedStdoutStream.mark(byteBuffer);
                    bufferedStderrStream.mark(byteBuffer);

                    String stderr = IOUtils.toString(bufferedStderrStream);
                    if (stderr != null && !stderr.isEmpty()) {
                        throw new ComponentException(FAILED_TO_SUBMIT_JOB + stderr);
                    }
                    stdout = IOUtils.toString(bufferedStdoutStream);

                    bufferedStdoutStream.reset();
                    bufferedStderrStream.reset();

                    // do it after termination because stdout and stderr is needed for component logic and not only for logging purposes.
                    // the delay is short because cluster job submission produces only few console output and terminates very quickly
                    for (String line : stdout.split(SystemUtils.LINE_SEPARATOR)) {
                        componentLog.toolStdout(line);
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            throw new ComponentException("Failed to submit job", e);
        }

        String jobId = extractJobIdFromQsubStdout(stdout);
        jobIds.add(jobId);
        jobsSubmittedLatch.get().countDown();
        componentLog.componentInfo("Id of submitted job: " + jobId);
        BlockingQueue<String> synchronousQueue = new SynchronousQueue<>();
        clusterService.addClusterJobStateChangeListener(jobId, new ClusterJobFinishListener(synchronousQueue));

        return synchronousQueue;
    }

    private String buildQsubCommand(String path) throws ComponentException {
        String jobScript = ClusterComponentConstants.JOB_SCRIPT_NAME;
        if (isJobScriptProvidedWithinInputDir) {
            jobScript = StringUtils.format("input%s%s", SLASH, jobScript);
        } else {
            for (int i = 0; i < path.split(SLASH).length; i++) {
                jobScript = StringUtils.format("..%s%s", SLASH, jobScript);
            }
        }
        return buildQsubCommand(path, jobScript);
    }

    private String buildQsubCommand(String path, String jobScript) throws ComponentException {
        switch (queuingSystem) {
        case TORQUE:
            return buildTorqueQsubCommand(path, jobScript);
        case SGE:
            return buildSgeQsubCommand(path, jobScript);
        default:
            throw new ComponentException("Queuing system not supported: " + queuingSystem.name());
        }
    }

    private String buildQsubMainCommand() {
        String qsubCommand = ClusterQueuingSystemConstants.COMMAND_QSUB;
        // with Java 8 this can be improved by Map.getOrDefault()(
        if (pathsToQueuingSystemCommands.get(ClusterQueuingSystemConstants.COMMAND_QSUB) != null) {
            qsubCommand = pathsToQueuingSystemCommands.get(ClusterQueuingSystemConstants.COMMAND_QSUB)
                + ClusterQueuingSystemConstants.COMMAND_QSUB;
        }
        return qsubCommand;
    }

    private String buildTorqueQsubCommand(String path, String jobScript) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("cd " + path);
        strBuilder.append(" && ");
        strBuilder.append(buildQsubMainCommand());
        strBuilder.append(" -d $PWD");
        strBuilder.append(" ");
        strBuilder.append(jobScript);
        return strBuilder.toString();
    }

    private String buildSgeQsubCommand(String path, String scriptFileName) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("cd " + path);
        strBuilder.append(" && ");
        strBuilder.append(buildQsubMainCommand());
        strBuilder.append(" -wd $PWD");
        strBuilder.append(" ");
        strBuilder.append(scriptFileName);
        return strBuilder.toString();
    }

    private String extractJobIdFromQsubStdout(String stdout) throws ComponentException {
        switch (queuingSystem) {
        case TORQUE:
            return extractJobIdFromTorqueQsubStdout(stdout);
        case SGE:
            return extractJobIdFromSgeQsubStdout(stdout);
        default:
            throw new ComponentException("Queuing system not supported: " + queuingSystem.name());
        }
    }

    private String extractJobIdFromTorqueQsubStdout(String stdout) throws ComponentException {
        Matcher matcher = Pattern.compile("\\d+\\.\\S*").matcher(stdout);
        if (matcher.find()) {
            return matcher.group();
        } else {
            matcher = Pattern.compile("\\d+").matcher(stdout);
            if (matcher.find()) {
                return matcher.group();
            } else {
                throw new ComponentException(FAILED_TO_SUBMIT_JOB + stdout);
            }
        }
    }

    private String extractJobIdFromSgeQsubStdout(String stdout) throws ComponentException {
        Matcher matcher = Pattern.compile("\\d+").matcher(stdout);
        if (matcher.find()) {
            return matcher.group();
        } else {
            throw new ComponentException(FAILED_TO_SUBMIT_JOB + stdout);
        }
    }

    private void downloadDirectoriesAndSendToOutputsOnJobFinished(Queue<BlockingQueue<String>> queues) throws ComponentException {

        CallablesGroup<ComponentException> callablesGroup =
            ConcurrencyUtils.getFactory().createCallablesGroup(ComponentException.class);

        int i = 0;
        for (BlockingQueue<String> queue : queues) {
            final BlockingQueue<String> queueSnapshot = queue;
            final int jobSnapshot = i++;
            callablesGroup.add(new Callable<ComponentException>() {

                @Override
                @TaskDescription("Wait for Job termination, check for failure, and download output directory afterwards")
                public ComponentException call() throws Exception {
                    try {
                        if (queueSnapshot.take().equals(ClusterComponentConstants.CLUSTER_FETCHING_FAILED)) {
                            return new ComponentException(FAILED_TO_WAIT_FOR_JOB_TO_BECOME_COMPLETED);
                        }
                    } catch (InterruptedException e) {
                        return new ComponentException("Interrupted while waiting for job termination", e);
                    }
                    try {
                        if (!isCancelled.get()) {
                            checkIfClusterJobSucceeded(jobSnapshot);
                            downloadDirectoryAndSendToOutput(jobSnapshot);
                        }
                    } catch (ComponentException e) {
                        return e;
                    }
                    return null;
                }
            });
        }

        List<ComponentException> exceptions = callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                // should never happen
                log.warn("Illegal state: Uncaught exception from Callable", e);
            }
        });

        for (ComponentException e : exceptions) {
            if (e != null) {
                log.error("Exception caught when downloading directories: " + e.getMessage());
            }
        }

        for (ComponentException e : exceptions) {
            if (e != null) {
                throw e;
            }
        }

    }

    private void checkIfClusterJobSucceeded(int job) throws ComponentException {

        String message = StringUtils.format("Failed to determine if cluster job %d succeeded - assumed that it does"
            + " to avoid false negatives", job);
        String path = getOutputFolderPath(job);
        String command = StringUtils.format("ls %s", path);
        try {
            synchronized (executorLock) {
                executor.start(command);
                try (InputStream stdoutStream = executor.getStdout();
                    InputStream stderrStream = executor.getStderr()) {
                    executor.waitForTermination();
                    if (!IOUtils.toString(stderrStream).isEmpty()) {
                        componentLog.componentError(StringUtils.format("Failed to execute command '%s' on %s: %s", command,
                            sshConfiguration.getDestinationHost(), IOUtils.toString(stderrStream)));
                        componentLog.componentError(message);
                    } else if (IOUtils.toString(stdoutStream).contains(FAILED_FILE_NAME)) {
                        String errorMessage = "N/A";
                        File file = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("out-" + job);
                        try {
                            executor.downloadFileFromWorkdir(path + SLASH + FAILED_FILE_NAME, file);
                            errorMessage = FileUtils.readFileToString(file);
                        } catch (IOException e) {
                            componentLog.componentError(StringUtils.format("Failed to download file '%s' "
                                + "- error message could not be extracted", FAILED_FILE_NAME));
                        }
                        throw new ComponentException(StringUtils.format("Cluster job %d failed with message: %s", job, errorMessage));
                    } else {
                        componentLog.componentInfo(StringUtils.format("Cluster job %d succeeded", job));
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            componentLog.componentError(message + ": " + e.getMessage());
            log.error(message, e);
        }
    }

    private void downloadDirectoryAndSendToOutput(int job) throws ComponentException {

        String message = "Downloading output directory failed: ";
        String path = getOutputFolderPath(job);
        try {
            File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
            componentLog.componentInfo("Downloading output directory: " + path);
            upDownloadSemaphore.acquire();
            executor.downloadDirectoryFromWorkdir(path, dir);
            upDownloadSemaphore.release();
            File outputDir = new File(dir, OUTPUT_FOLDER_NAME + "-" + job);
            File origDir = new File(dir, OUTPUT_FOLDER_NAME);
            if (!origDir.renameTo(outputDir)) {
                throw new IOException(StringUtils.format("Failed to rename directory for an unknown reason: %s->%s", origDir, outputDir));
            }
            DirectoryReferenceTD dirRef = dataManagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext,
                outputDir, outputDir.getName());
            componentContext.writeOutput(ClusterComponentConstants.OUTPUT_JOBOUTPUTS, dirRef);
            componentLog.componentInfo("Output directory downloaded: " + path + ". Will be sent as: " + outputDir.getName());
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
        } catch (IOException | InterruptedException e) {
            throw new ComponentException(message + path, e);
        }
    }

    private String getJobFolderPath(int job) {
        return StringUtils.format(PATH_PATTERN, iteration, job);
    }

    private String getOutputFolderPath(int job) {
        return getJobFolderPath(job) + SLASH + OUTPUT_FOLDER_NAME;
    }

}

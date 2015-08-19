/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.cluster.execution;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.components.cluster.execution.internal.ClusterJobFinishListener;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystemConstants;
import de.rcenvironment.core.utils.cluster.ClusterService;
import de.rcenvironment.core.utils.cluster.ClusterServiceManager;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfigurationFactory;
import de.rcenvironment.core.utils.ssh.jsch.executor.context.JSchExecutorContext;

/**
 * Submits a job to a cluster batch system with upload and download of directories.
 * 
 * @author Doreen Seider
 */
public class ClusterComponent extends DefaultComponent {

    private static final String FAILED_FILE_NAME = "cluster_job_failed";

    private static final String SLASH = "/";

    private static final String OUTPUT_FOLDER_NAME = "output";

    private static final String AT = "@";

    private static final int UNKNOWN = -1;

    private static final String PATH_PATTERN = "iteration-%d/cluster-job-%d";

    private static final Log LOG = LogFactory.getLog(ClusterComponent.class);

    private static Log log = LogFactory.getLog(ClusterComponent.class);

    private ComponentContext componentContext;

    private ClusterServiceManager clusterServiceManager;

    private ComponentDataManagementService dataManagementService;

    private ClusterComponentConfiguration clusterConfiguration;

    private SshSessionConfiguration sshConfiguration;

    private JSchExecutorContext context;

    private CommandLineExecutor executor;

    private ClusterService clusterService;

    private ClusterQueuingSystem queuingSystem;

    private Map<String, String> pathsToQueuingSystemCommands;

    private int jobCount = UNKNOWN;

    private boolean considerSharedInputDir = true;

    private int iteration = 0;

    private Semaphore upDownloadSemaphore;

    private boolean isJobScriptProvidedWithinInputDir;

    private Map<String, Deque<TypedDatum>> inputValues = new HashMap<String, Deque<TypedDatum>>();

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        clusterServiceManager = componentContext.getService(ClusterServiceManager.class);
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);

        ConfigurationService configurationService = componentContext.getService(ConfigurationService.class);
        // TODO 6.0.0 review: preliminary path
        clusterConfiguration = new ClusterComponentConfiguration(
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
            log.debug("Session established: " + authUser + AT + host + ":" + port);
        } catch (IOException e) {
            throw new ComponentException("Establishing connection to remote host failed.", e);
        } catch (ValidationFailureException e) {
            throw new ComponentException("Validation of passed parameters failed", e);
        }

        try {
            executor = context.setUpSandboxedExecutor();
            log.debug("Remote sandbox created: " + authUser + AT + host + ":" + port);
        } catch (IOException e) {
            throw new ComponentException("Setting up remote sandbox failed", e);
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
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (!inputValues.containsKey(inputName)) {
                inputValues.put(inputName, new LinkedList<TypedDatum>());
            }
            inputValues.get(inputName).add(componentContext.readInput(inputName));
        }
        if (jobCount == UNKNOWN && inputValues.containsKey(ClusterComponentConstants.INPUT_JOBCOUNT)) {
            jobCount = (int) ((IntegerTD) inputValues.get(ClusterComponentConstants.INPUT_JOBCOUNT).poll()).getIntValue();
        }
        if (jobCount != UNKNOWN && inputValues.containsKey(ClusterComponentConstants.INPUT_JOBINPUTS)
            && inputValues.get(ClusterComponentConstants.INPUT_JOBINPUTS).size() >= jobCount
            && (!considerSharedInputDir || (inputValues.containsKey(ClusterComponentConstants.INPUT_SHAREDJOBINPUT)
            && inputValues.get(ClusterComponentConstants.INPUT_SHAREDJOBINPUT).size() >= 1))) {

            // consume inputs
            List<DirectoryReferenceTD> inputDirs = new ArrayList<DirectoryReferenceTD>();
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

            jobCount = UNKNOWN;
            iteration++;
        }
    }

    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        deleteSandboxIfNeeded();
    }

    private void deleteSandboxIfNeeded() {
        Boolean deleteSandbox = Boolean.valueOf(componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_DELETESANDBOX));

        if (deleteSandbox) {
            String sandbox = executor.getWorkDirPath();
            try {
                // delete here explicitly as context.tearDownSandbox(executor) doesn't support -r option for safety reasons
                executor.start("rm -r " + sandbox);
                context.tearDownSession();
                log.debug("Remote sandbox deleted: " + sandbox);
            } catch (IOException e) {
                log.error("Deleting remote sandbox failed: " + sandbox, e);
            }
        }
    }

    private void uploadJobScript() {
        String message = "Uploading job script failed";
        try {
            File jobFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                ClusterComponentConstants.JOB_SCRIPT_NAME);
            FileUtils.write(jobFile, componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT));
            logInfoMetaInformation("Uploading job script: " + jobFile.getName());
            upDownloadSemaphore.acquire();
            executor.uploadFileToWorkdir(jobFile, ".");
            upDownloadSemaphore.release();
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(jobFile);
            logInfoMetaInformation("Job script uploaded: " + jobFile.getName());
        } catch (IOException e) {
            logErrorMetaInformation(message);
            throw new RuntimeException(message, e);
        } catch (InterruptedException e) {
            logErrorMetaInformation(message);
            throw new RuntimeException(message, e);
        }

    }

    private void uploadInputDirectories(List<DirectoryReferenceTD> inputDirs, final DirectoryReferenceTD sharedInputDir) {

        logInfoMetaInformation("Start uploading input directories...");
        int count = 0;
        CallablesGroup<RuntimeException> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(RuntimeException.class);

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
                        logInfoMetaInformation("Start uploading shared input directory...");
                        uploadInputDirectory(sharedInputDir, "", "cluster-job-shared-input");
                        logInfoMetaInformation("Uploading shared input directory finished");
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
                LOG.warn("Illegal state: Uncaught exception from Callable", e);
            }
        });

        for (RuntimeException e : exceptions) {
            if (e != null) {
                LOG.error("Exception caught when uploading directories: " + e.getMessage());
            }
        }

        for (RuntimeException e : exceptions) {
            if (e != null) {
                throw e;
            }
        }

        logInfoMetaInformation("Uploading input directories finished");
    }

    private void uploadInputDirectory(DirectoryReferenceTD jobDir, String directoryParent, String dirName) {
        String message = "Uploading directory failed: ";
        try {
            File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
            dataManagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext, jobDir,
                dir);
            logInfoMetaInformation("Uploading directory: " + jobDir.getDirectoryName());
            File inputDir = new File(dir, dirName);
            new File(dir, jobDir.getDirectoryName()).renameTo(inputDir);
            upDownloadSemaphore.acquire();
            executor.uploadDirectoryToWorkdir(inputDir, "iteration-" + iteration + directoryParent);
            upDownloadSemaphore.release();
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
            logInfoMetaInformation("Directory uploaded: " + jobDir.getDirectoryName());
        } catch (IOException e) {
            logErrorMetaInformation(message + jobDir.getDirectoryName());
            throw new RuntimeException(message + jobDir.getDirectoryName(), e);
        } catch (InterruptedException e) {
            logErrorMetaInformation(message + jobDir.getDirectoryName());
            throw new RuntimeException(message + jobDir.getDirectoryName(), e);
        }
    }

    private Queue<BlockingQueue<String>> submitJobs() {
        Queue<BlockingQueue<String>> blockingQueues = new LinkedList<BlockingQueue<String>>();
        for (int i = 0; i < jobCount; i++) {
            blockingQueues.add(submitJob(i));
        }

        return blockingQueues;
    }

    private BlockingQueue<String> submitJob(int job) {

        final int byteBuffer = 10000;

        String stdout;

        try {
            String mkDirOutputCommand = "mkdir " + getOutputFolderPath(job) + " ";
            executor.start(mkDirOutputCommand);
            executor.waitForTermination();
            String qsubCommand = buildQsubCommand(SLASH + getJobFolderPath(job));
            executor.start(qsubCommand);
            LOG.debug("Submitted job to cluster: " + ClusterComponentConstants.JOB_SCRIPT_NAME
                + " from " + getJobFolderPath(job));
            InputStream stdoutStream = executor.getStdout();
            InputStream stderrStream = executor.getStderr();
            executor.waitForTermination();

            BufferedInputStream bufferedStdoutStream = new BufferedInputStream(stdoutStream);
            BufferedInputStream bufferedStderrStream = new BufferedInputStream(stderrStream);

            bufferedStdoutStream.mark(byteBuffer);
            bufferedStderrStream.mark(byteBuffer);

            String stderr = IOUtils.toString(bufferedStderrStream);
            if (stderr != null && !stderr.isEmpty()) {
                componentContext.printConsoleLine(stderr, ConsoleRow.Type.STDERR);
                throw new RuntimeException("Submitting job failed: " + stderr);
            }
            stdout = IOUtils.toString(bufferedStdoutStream);

            bufferedStdoutStream.reset();
            bufferedStderrStream.reset();

            // do it after termination because stdout and stderr is needed for component logic and not only for logging purposes.
            // the delay is short because cluster job submission produces only few console output and terminates very quickly
            for (String line : stdout.split("\n")) {
                componentContext.printConsoleLine(line, ConsoleRow.Type.STDOUT);
            }

            IOUtils.closeQuietly(stdoutStream);
            IOUtils.closeQuietly(stderrStream);

            IOUtils.closeQuietly(bufferedStdoutStream);
            IOUtils.closeQuietly(bufferedStderrStream);

        } catch (IOException e) {
            throw new RuntimeException("Executing job sumbission failed", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Waiting for termination of job sumbission failed", e);
        }

        String jobId = extractJobIdFromQsubStdout(stdout);

        LOG.debug("Job id of submitted job: " + jobId);
        BlockingQueue<String> synchronousQueue = new SynchronousQueue<String>();
        clusterService.addClusterJobStateChangeListener(jobId, new ClusterJobFinishListener(synchronousQueue));

        return synchronousQueue;
    }

    private String buildQsubCommand(String path) {
        String jobScript = ClusterComponentConstants.JOB_SCRIPT_NAME;
        if (isJobScriptProvidedWithinInputDir) {
            jobScript = path + "/input/" + jobScript;
            jobScript = jobScript.replaceFirst(SLASH, "");
        }
        return buildQsubCommand(path, jobScript);
    }

    private String buildQsubCommand(String path, String jobScript) {
        switch (queuingSystem) {
        case TORQUE:
            return buildTorqueQsubCommand(path, jobScript);
        case SGE:
            return buildSgeQsubCommand(path, jobScript);
        default:
            throw new RuntimeException("Queuing system not supported: " + queuingSystem.name());
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
        StringBuffer buffer = new StringBuffer(buildQsubMainCommand());
        buffer.append(" -d ");
        buffer.append(executor.getWorkDirPath() + path);
        buffer.append(" " + executor.getWorkDirPath() + "/");
        buffer.append(jobScript);
        return buffer.toString();
    }

    private String buildSgeQsubCommand(String path, String scriptFileName) {
        StringBuffer buffer = new StringBuffer(buildQsubMainCommand());
        buffer.append(" -wd ");
        buffer.append(executor.getWorkDirPath() + path);
        buffer.append(" ");
        buffer.append(scriptFileName);
        return buffer.toString();
    }

    private String extractJobIdFromQsubStdout(String stdout) {
        switch (queuingSystem) {
        case TORQUE:
            return extractJobIdFromTorqueQsubStdout(stdout);
        case SGE:
            return extractJobIdFromSgeQsubStdout(stdout);
        default:
            throw new RuntimeException("Queuing system not supported: " + queuingSystem.name());
        }
    }

    private String extractJobIdFromTorqueQsubStdout(String stdout) {
        Matcher matcher = Pattern.compile("\\d+\\.\\w+").matcher(stdout);
        if (matcher.find()) {
            return matcher.group();
        } else {
            matcher = Pattern.compile("\\d+").matcher(stdout);
            if (matcher.find()) {
                return matcher.group();
            } else {
                throw new RuntimeException("Executing job sumbission failed: " + stdout);
            }
        }
    }

    private String extractJobIdFromSgeQsubStdout(String stdout) {
        Matcher matcher = Pattern.compile("\\d+").matcher(stdout);
        if (matcher.find()) {
            return matcher.group();
        } else {
            throw new RuntimeException("Executing job sumbission failed: " + stdout);
        }
    }

    private void downloadDirectoriesAndSendToOutputsOnJobFinished(Queue<BlockingQueue<String>> queues) {

        CallablesGroup<RuntimeException> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(RuntimeException.class);

        int i = 0;
        for (BlockingQueue<String> queue : queues) {
            final BlockingQueue<String> queueSnapshot = queue;
            final int jobSnapshot = i++;
            callablesGroup.add(new Callable<RuntimeException>() {

                @Override
                @TaskDescription("Wait for Job termination, check for failure, and download output directory afterwards")
                public RuntimeException call() throws Exception {
                    try {
                        try {
                            if (queueSnapshot.take().equals(ClusterComponentConstants.CLUSTER_FETCHING_FAILED)) {
                                throw new RuntimeException("Waiting for job become completed failed");
                            }
                            checkIfClusterJobSucceeded(jobSnapshot);
                            downloadDirectoryAndSendToOutput(jobSnapshot);
                        } catch (InterruptedException e) {
                            throw new RuntimeException("Waiting for job become completed was interrupted", e);
                        }
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
                LOG.warn("Illegal state: Uncaught exception from Callable", e);
            }
        });

        for (RuntimeException e : exceptions) {
            if (e != null) {
                LOG.error("Exception caught when downloading directories: " + e.getMessage());
            }
        }

        for (RuntimeException e : exceptions) {
            if (e != null) {
                throw e;
            }
        }

    }

    // synchronized to prevent starting executor in parallel
    private synchronized void checkIfClusterJobSucceeded(int job) throws ComponentException {

        String message = String.format("Failed to determine if cluster job %d succeeded. Assumed that it does, to avoid false negatives.",
            job);
        String path = getOutputFolderPath(job);
        String command = String.format("ls %s", path);
        try {
            executor.start(command);
            InputStream stdoutStream = executor.getStdout();
            InputStream stderrStream = executor.getStderr();
            executor.waitForTermination();
            if (!IOUtils.toString(stderrStream).isEmpty()) {
                log.error(String.format("Failed to execute command '%s' on %s: %s", command,
                    sshConfiguration.getDestinationHost(), IOUtils.toString(stderrStream)));
                log.error(message);
            } else if (IOUtils.toString(stdoutStream).contains(FAILED_FILE_NAME)) {
                String errorMessage = "N/A";
                File file = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("out-" + job);
                try {
                    executor.downloadFileFromWorkdir(path + SLASH + FAILED_FILE_NAME, file);
                    errorMessage = FileUtils.readFileToString(file);
                } catch (IOException e) {
                    log.error(String.format("Downloading file '%s' failed. Error message could not extracted.", FAILED_FILE_NAME));
                }
                logErrorMetaInformation(String.format("Cluster job '%d' failed with message: %s", job, errorMessage));
                throw new ComponentException(String.format("Cluster job '%d' failed with message: %s", job, errorMessage));
            } else {
                logInfoMetaInformation(String.format("Cluster job %d succeeded", job));
            }
        } catch (IOException e) {
            log.error(message, e);
        } catch (InterruptedException e) {
            log.error(message, e);
        }
    }

    private void downloadDirectoryAndSendToOutput(int job) {

        String message = "Downloading output directory failed: ";
        String path = getOutputFolderPath(job);
        try {
            File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
            logInfoMetaInformation("Downloading output directory: " + path);
            upDownloadSemaphore.acquire();
            executor.downloadDirectoryFromWorkdir(path, dir);
            upDownloadSemaphore.release();
            File outputDir = new File(dir, OUTPUT_FOLDER_NAME + "-" + job);
            new File(dir, OUTPUT_FOLDER_NAME).renameTo(outputDir);
            DirectoryReferenceTD dirRef = dataManagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext,
                outputDir, outputDir.getName());
            componentContext.writeOutput(ClusterComponentConstants.OUTPUT_JOBOUTPUTS, dirRef);
            logInfoMetaInformation("Output directory downloaded: " + path + ". Will be send as: " + outputDir.getName());
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
        } catch (IOException e) {
            throw new RuntimeException(message + path, e);
        } catch (InterruptedException e) {
            throw new RuntimeException(message + path, e);
        }
    }

    private String getJobFolderPath(int job) {
        return String.format(PATH_PATTERN, iteration, job);
    }

    private String getOutputFolderPath(int job) {
        return getJobFolderPath(job) + SLASH + OUTPUT_FOLDER_NAME;
    }

    private void logInfoMetaInformation(String text) {
        log.info(text);
        logMetaInformation(text);
    }

    private void logErrorMetaInformation(String text) {
        log.error(text);
        logMetaInformation(text);
    }

    private void logMetaInformation(String text) {
        componentContext.printConsoleLine(text, ConsoleRow.Type.COMPONENT_OUTPUT);
    }

    protected void activate(org.osgi.service.component.ComponentContext ctx) {
        // TODO delete?
    }
}

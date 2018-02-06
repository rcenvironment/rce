/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Verifies that the actual behavior of workflows executed with "wf verify" match the the expected behavior given by certain indicator and
 * text files.
 * 
 * @author Doreen Seider
 * @author Hendrik Abbenhaus
 */
public final class HeadlessWorkflowExecutionVerification
    implements HeadlessWorkflowExecutionVerificationRecorder, HeadlessWorkflowExecutionVerificationResult {

    /** Name of directory with workflows that are expected to fail. */
    public static final String FAILURE_DIR_NAME = "failure";
    
    /** Date-Format used for timestamps in results log. */
    public static final String DATE_FORMAT = "yyyy-MM-dd - HH:mm:ss";

    /** File name suffix for files that contain expected log messages. */
    public static final String FILE_SUFFIX_EXPECTED_LOG = ".log.expected";

    /** File name suffix for files that contain prohibited log messages. */
    public static final String FILE_SUFFIX_PROHIBITED_LOG = ".log.prohibited";

    private static final String EMPTY_COLLECTION_STRING = "-";
    
    private static final int THOUSAND = 1000;

    private String wfFileRootDir = null;

    private final List<File> wfFilesSubmitted;

    private final int parallelRuns;

    private final int sequentialRuns;

    // measured in seconds as the current approach to determine workflow termination (via notification) doesn't allow more accuracy
    private Map<File, List<Long>> wfsWithExecutionDurationSec = new HashMap<>();

    private Map<File, List<String>> wfsWithExpectedLogMessages = new HashMap<>();

    private Map<File, List<String>> wfsWithProhibitedLogMessages = new HashMap<>();

    private int wfsExpectedToFinishCount;

    private int wfsExpectedToFailCount;

    private List<File> wfsExpectedToFail = new ArrayList<>();

    private int resultCount = 0;

    private int wfsFinishedAsExpectedCount = 0;

    private int wfsFailedAsExpectedCount = 0;

    private int wfsCanceledCount = 0;

    private int wfsWithErrorCount = 0;

    private int wfsWithUnexpectedFinalStateCount = 0;

    private int wfsWithUnexpectedMissingLogMessageCount = 0;

    private int wfsWithUnexpectedProhibitedLogMessageCount = 0;

    private Map<File, FinalWorkflowState> wfsWithUnexpectedFinalState = new HashMap<>();

    private Map<File, String> wfsWithUnexpectedMissingLogMessage = new HashMap<>();

    private Map<File, String> wfsWithUnexpectedProhibitedLogMessage = new HashMap<>();

    private List<File> wfsCanceled = new ArrayList<>();

    private Map<File, String> wfsWithError = new HashMap<>();

    private List<File> wfRelatedFilesToDelete = new ArrayList<>();

    private boolean verified = true;
    
    private Date startTime = null;
    
    private Date endTime = null;

    private HeadlessWorkflowExecutionVerification(List<File> wfFiles, int parallelRuns, int sequentialRuns) {
        this.wfFilesSubmitted = wfFiles;
        this.parallelRuns = parallelRuns;
        this.sequentialRuns = sequentialRuns;
        if (!wfFiles.isEmpty()) {
            File parentFile = wfFiles.get(0).getParentFile();
            if (!parentFile.getName().equals(FAILURE_DIR_NAME)) {
                wfFileRootDir = parentFile.getAbsolutePath();
            } else {
                wfFileRootDir = parentFile.getParentFile().getAbsolutePath();
            }
            // was originally written to console output; moved to log, as it should usually be irrelevant
            LogFactory.getLog(getClass()).debug("Using workflow verification root dir " + wfFileRootDir);
        }
        for (File wfFile : wfFiles) {
            wfsWithExecutionDurationSec.put(wfFile, new ArrayList<Long>());
        }
        wfsExpectedToFinishCount = wfFiles.size() * parallelRuns * sequentialRuns;
    }

    /**
     * Creates an initialized instance of {@link HeadlessWorkflowExecutionVerificationRecorder}.
     * 
     * @param wfFiles the workflow files to consider
     * @param parallelRuns number of expected parallel runs
     * @param sequentialRuns number of expected sequential runs
     * @return initialized instance of {@link HeadlessWorkflowExecutionVerificationRecorder}
     * @throws IOException if initializing instance of {@link HeadlessWorkflowExecutionVerificationRecorder} fails (mainly because of
     *         violated conventions)
     */
    public static HeadlessWorkflowExecutionVerificationRecorder createAndInitializeInstance(List<File> wfFiles, int parallelRuns,
        int sequentialRuns) throws IOException {
        HeadlessWorkflowExecutionVerification verification =
            new HeadlessWorkflowExecutionVerification(wfFiles, parallelRuns, sequentialRuns);
        verification.initialize();
        return verification;
    }

    /**
     * Initializes the expected behavior of workflows to execute by scanning for certain files.
     * 
     * [wf file name without ending].failing -> indicates that the related workflow is expected to fail [wf file name without
     * ending].log.expected -> contains log messages (lines) that must be contained in the workflow log [wf file name without
     * ending].log.prohibited -> contains log messages (lines) that must not be contained in the workflow log
     * 
     * @throws IOException if accessing the files above failed
     */
    private void initialize() throws IOException {
        for (File wfFile : wfFilesSubmitted) {
            String wfFileNameWithoutEnding = wfFile.getName().replace(WorkflowConstants.WORKFLOW_FILE_ENDING, "");
            if (wfFile.getParentFile().getName().equals(FAILURE_DIR_NAME)) {
                wfsExpectedToFail.add(wfFile);
            }
            File wfExpectedLogFile = new File(wfFile.getParentFile(), wfFileNameWithoutEnding + FILE_SUFFIX_EXPECTED_LOG);
            if (wfExpectedLogFile.exists()) {
                wfsWithExpectedLogMessages.put(wfFile, FileUtils.readLines(wfExpectedLogFile));
            }
            File wfProhibitedLogFile = new File(wfFile.getParentFile(), wfFileNameWithoutEnding + FILE_SUFFIX_PROHIBITED_LOG);
            if (wfProhibitedLogFile.exists()) {
                wfsWithProhibitedLogMessages.put(wfFile, FileUtils.readLines(wfProhibitedLogFile));
            }
        }
        wfsExpectedToFailCount = wfsExpectedToFail.size() * parallelRuns * sequentialRuns;
        wfsExpectedToFinishCount = wfsExpectedToFinishCount - wfsExpectedToFailCount;
    }

    @Override
    public synchronized void addWorkflowError(File wfFile, String errorMessage) {
        wfsWithError.put(wfFile, errorMessage);
        wfsWithErrorCount++;
        resultCount++;
        verifyOverall();
    }

    @Override
    public synchronized boolean addWorkflowExecutionResult(File wfFile, File[] wfLogFiles, FinalWorkflowState finalState,
        long executionDuration)
        throws IOException {
        if (!wfFilesSubmitted.contains(wfFile)) {
            throw new IllegalArgumentException("Given workflow file unknown: " + wfFile);
        }
        boolean behavedAsExpected = false;
        switch (finalState) {
        case FINISHED:
            if (!wfsExpectedToFail.contains(wfFile)) {
                wfsFinishedAsExpectedCount++;
                behavedAsExpected = verifyLog(wfFile, wfLogFiles, behavedAsExpected);
            } else {
                wfsWithUnexpectedFinalState.put(wfFile, finalState);
                wfsWithUnexpectedFinalStateCount++;
            }
            break;
        case FAILED:
            if (wfsExpectedToFail.contains(wfFile)) {
                wfsFailedAsExpectedCount++;
                behavedAsExpected = verifyLog(wfFile, wfLogFiles, behavedAsExpected);
            } else {
                wfsWithUnexpectedFinalState.put(wfFile, finalState);
                wfsWithUnexpectedFinalStateCount++;
            }
            break;
        case CANCELLED:
            wfsCanceled.add(wfFile);
            wfsCanceledCount++;
            break;
        default:
            throw new IllegalArgumentException("Given final workflow state unknown: " + finalState);
        }
        resultCount++;
        verifyOverall();
        wfsWithExecutionDurationSec.get(wfFile).add(executionDuration / THOUSAND);
        if (behavedAsExpected) {
            registerWorkflowRelatedFilesForDeletion(wfFile);

        }
        return behavedAsExpected;
    }

    private void registerWorkflowRelatedFilesForDeletion(File wfFile) {
        wfRelatedFilesToDelete.add(wfFile);
        File backupFile = new File(wfFile.getAbsolutePath().replace(WorkflowConstants.WORKFLOW_FILE_ENDING, "")
            + WorkflowConstants.WORKFLOW_FILE_BACKUP_SUFFIX + WorkflowConstants.WORKFLOW_FILE_ENDING);
        if (backupFile.exists()) {
            wfRelatedFilesToDelete.add(backupFile);
        }
        if (wfsWithExpectedLogMessages.containsKey(wfFile)) {
            wfRelatedFilesToDelete.add(new File(wfFile.getParentFile(),
                wfFile.getName().replace(WorkflowConstants.WORKFLOW_FILE_ENDING, "") + FILE_SUFFIX_EXPECTED_LOG));
        }
        if (wfsWithProhibitedLogMessages.containsKey(wfFile)) {
            wfRelatedFilesToDelete.add(new File(wfFile.getParentFile(),
                wfFile.getName().replace(WorkflowConstants.WORKFLOW_FILE_ENDING, "") + FILE_SUFFIX_PROHIBITED_LOG));
        }
    }
    
    

    @Override
    public List<File> getWorkflowRelatedFilesToDelete() {
        return wfRelatedFilesToDelete;
    }

    private boolean verifyLog(File wfFile, File[] wfLogFiles, boolean behavedAsExpected) throws IOException {
        for (File wfLogFile : wfLogFiles) {
            behavedAsExpected = verifySingleLogFile(wfFile, wfLogFile);
            if (!behavedAsExpected) {
                break;
            }
        }
        return behavedAsExpected;
    }

    private boolean verifySingleLogFile(File wfFile, File wfLogFile) throws IOException {
        boolean logVerified = true;
        String actualLogMessages = FileUtils.readFileToString(wfLogFile);
        if (wfsWithExpectedLogMessages.containsKey(wfFile)) {
            for (String expectedLogLine : wfsWithExpectedLogMessages.get(wfFile)) {
                boolean missed = !actualLogMessages.contains(expectedLogLine);
                if (missed) {
                    wfsWithUnexpectedMissingLogMessage.put(wfFile, expectedLogLine);
                    wfsWithUnexpectedMissingLogMessageCount++;
                }
                logVerified &= !missed;
            }
        }
        if (wfsWithProhibitedLogMessages.containsKey(wfFile)) {
            for (String prohibitedLogLine : wfsWithProhibitedLogMessages.get(wfFile)) {
                boolean contained = actualLogMessages.contains(prohibitedLogLine);
                if (contained) {
                    wfsWithUnexpectedProhibitedLogMessage.put(wfFile, prohibitedLogLine);
                    wfsWithUnexpectedProhibitedLogMessageCount++;
                }
                logVerified &= !contained;
            }
        }
        verified &= logVerified;
        return logVerified;
    }

    private boolean verifyOverall() {
        if (verified) {
            verified &= wfsWithError.isEmpty() && wfsCanceled.isEmpty()
                && wfsWithUnexpectedFinalState.isEmpty() && wfsWithUnexpectedMissingLogMessage.isEmpty()
                && wfsWithUnexpectedProhibitedLogMessage.isEmpty();
        }
        return isVerified();
    }

    @Override
    public synchronized boolean isVerified() {
        return verified && wfsExpectedToFinishCount + wfsExpectedToFailCount == resultCount;
    }

    @Override
    public synchronized String getVerificationReport() {
        StringBuilder builder = new StringBuilder();

        builder.append("Workflow Statistics\n");

        builder.append(StringUtils.format("- Finished (as expected): %d/%d,"
            + " Failed (as expected): %d/%d, Canceled: %d/0, Error: %d/0, Unexpected final state: %d/0 [Total: %d/%d]\n",
            wfsFinishedAsExpectedCount, wfsExpectedToFinishCount, wfsFailedAsExpectedCount, wfsExpectedToFailCount, wfsCanceledCount,
            wfsWithErrorCount, wfsWithUnexpectedFinalStateCount, resultCount, wfsExpectedToFinishCount + wfsExpectedToFailCount));

        builder.append(StringUtils.format(
            "- Canceled (%d): %s\n- With error (%d): %s\n- With unexpected final state (%d): %s"
                + "\n- With missing log message (%d): %s\n- With prohibited log message (%d): %s\n",
            wfsCanceledCount, convertFileListToString(wfsCanceled), wfsWithErrorCount,
            convertFileMapToString(wfsWithError), wfsWithUnexpectedFinalStateCount,
            convertFileMapToString(wfsWithUnexpectedFinalState), wfsWithUnexpectedMissingLogMessageCount,
            convertFileMapToString(wfsWithUnexpectedMissingLogMessage), wfsWithUnexpectedProhibitedLogMessageCount,
            convertFileMapToString(wfsWithUnexpectedProhibitedLogMessage)));

        builder.append("Durations per workflow [sec]\n");
        int sec = 0;
        for (Entry<String, Object> durationEntry : reduceFilePathToFileName(wfsWithExecutionDurationSec).entrySet()) {
            String duration = convertListToString((List<? extends Object>) durationEntry.getValue());
            builder.append(StringUtils.format("- %s: %s\n", durationEntry.getKey(), duration));
            int newValue;
            try {
                newValue = Integer.parseInt(duration);
            } catch (NumberFormatException e){
                newValue = 0;
            }
            sec += newValue;
        }
        builder.append(StringUtils.format("Durations (overall)\n"));
        builder.append(StringUtils.format("- Start time: %s \n", new SimpleDateFormat(DATE_FORMAT).format(startTime)));
        builder.append(StringUtils.format("- End time: %s \n", new SimpleDateFormat(DATE_FORMAT).format(endTime)));
        builder.append(StringUtils.format("- Aggregated: %s sec\n", sec));
        int totaltime = (int) ((endTime.getTime() - startTime.getTime()) / THOUSAND);
        builder.append(StringUtils.format("- Total: %s sec\n", totaltime));
        
        String result;
        if (isVerified()) {
            result = "SUCCEEDED";
        } else {
            result = "FAILED";
        }
        builder.append("Summary Result\n");

        builder.append(StringUtils.format(
            "- Verification %s [%d workflow files, parallel: %d, sequential: %d]\n",
            result, wfFilesSubmitted.size(), parallelRuns, sequentialRuns));

        return builder.toString();
    }

    private SortedMap<String, Object> reduceFilePathToFileName(Map<File, ? extends Object> fileMap) {
        SortedMap<String, Object> fileNameMap = new TreeMap<>();
        for (File file : fileMap.keySet()) {
            fileNameMap.put(file.getName(), fileMap.get(file));
        }
        return fileNameMap;
    }
    
    private String convertFileMapToString(Map<File, ? extends Object> fileMap) {
        if (fileMap.isEmpty()) {
            return EMPTY_COLLECTION_STRING;
        } else {
            return reduceFilePathToFileName(fileMap).toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();
        }
    }

    private SortedSet<String> reduceFilePathToFileName(List<File> fileList) {
        SortedSet<String> fileNameSet = new TreeSet<>();
        for (File file : fileList) {
            fileNameSet.add(file.getName());
        }
        return fileNameSet;
    }
    
    private String convertFileListToString(List<File> fileList) {
        return convertSetToString(reduceFilePathToFileName(fileList));
    }
    
    private String convertListToString(List<? extends Object> list) {
        if (list.isEmpty()) {
            return EMPTY_COLLECTION_STRING;
        } else {
            return list.toString().replaceAll("\\[", "").replaceAll("\\]", "").trim();
        }
    }
    
    private String convertSetToString(Set<? extends Object> set) {
        if (set.isEmpty()) {
            return EMPTY_COLLECTION_STRING;
        } else {
            return set.toString().replaceAll("\\[", "").replaceAll("\\]", "").trim();
        }
    }

    @Override
    public void setStartAndEndTime(Date start, Date end) {
        this.startTime = start;
        this.endTime = end;
    }


}

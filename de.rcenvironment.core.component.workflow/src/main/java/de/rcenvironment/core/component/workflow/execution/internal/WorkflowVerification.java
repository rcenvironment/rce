/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationBuilder.LogFolderFactory;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationResults;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationService;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerification;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationRecorder;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationResult;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.PrefixingTextOutForwarder;

public class WorkflowVerification {

    private TextOutputReceiver outputReceiver;

    private File workflowRootFile;

    private final List<File> wfFilesExpectedToSucceed = new LinkedList<>();

    private final List<File> wfFilesExpectedToFail = new LinkedList<>();

    private File placeholdersFile;

    private LogFolderFactory logfolderFactory;

    private int parallelRuns;

    private int sequentialRuns;

    private HeadlessWorkflowExecutionService.DisposalBehavior dispose;

    private HeadlessWorkflowExecutionService.DeletionBehavior delete;

    private HeadlessWorkflowExecutionService workflowExecutionService;

    private WorkflowVerificationService workflowVerificationService;

    public static final class Builder implements WorkflowVerificationBuilder {

        private final WorkflowVerification product = new WorkflowVerification();

        public Builder() {};
        
        @Override
        public WorkflowVerificationBuilder outputReceiver(TextOutputReceiver receiver) {
            product.outputReceiver = receiver;
            return this;
        }

        @Override
        public WorkflowVerificationBuilder workflowRootFile(File workflowRootFile) {
            product.workflowRootFile = workflowRootFile;
            return this;
        }

        @Override
        public WorkflowVerificationBuilder addWorkflowExpectedToSucceed(File workflowExpectedToSucceed) {
            product.wfFilesExpectedToSucceed.add(workflowExpectedToSucceed);
            return this;
        }

        @Override
        public WorkflowVerificationBuilder addWorkflowsExpectedToSucceed(Collection<File> workflowsExpectedToSucceed) {
            product.wfFilesExpectedToSucceed.addAll(workflowsExpectedToSucceed);
            return this;
        }

        @Override
        public WorkflowVerificationBuilder addWorkflowExpectedToFail(File workflowExpectedToFail) {
            product.wfFilesExpectedToFail.add(workflowExpectedToFail);
            return this;
        }

        @Override
        public WorkflowVerificationBuilder addWorkflowsExpectedToFail(Collection<File> workflowsExpectedToFail) {
            product.wfFilesExpectedToFail.addAll(workflowsExpectedToFail);
            return this;
        }

        @Override
        public WorkflowVerificationBuilder placeholdersFile(File placeholdersFileParam) {
            product.placeholdersFile = placeholdersFileParam;
            return this;
        }

        @Override
        public WorkflowVerificationBuilder logFileFactory(LogFolderFactory logFileFactory) {
            product.logfolderFactory = logFileFactory;
            return this;
        }

        @Override
        public WorkflowVerificationBuilder numberOfParallelRuns(int parallelRunsParam) {
            product.parallelRuns = parallelRunsParam;
            return this;
        }

        @Override
        public WorkflowVerificationBuilder numberOfSequentialRuns(int sequentialRunsParam) {
            product.sequentialRuns = sequentialRunsParam;
            return this;
        }

        @Override
        public WorkflowVerificationBuilder disposalBehavior(HeadlessWorkflowExecutionService.DisposalBehavior disposeParam) {
            product.dispose = disposeParam;
            return this;
        }

        @Override
        public WorkflowVerificationBuilder deletionBehavior(HeadlessWorkflowExecutionService.DeletionBehavior deleteParam) {
            product.delete = deleteParam;
            return this;
        }
        
        public WorkflowVerificationBuilder workflowVerificationService(WorkflowVerificationService service) {
            product.workflowVerificationService = service;
            return this;
        }
        
        @Override
        public WorkflowVerificationBuilder workflowExecutionService(HeadlessWorkflowExecutionService service) {
            product.workflowExecutionService = service;
            return this;
        }

        @Override
        public WorkflowVerificationResults verify() throws IOException {
            return product.verify();
        }

    }

    public WorkflowVerificationResults verify() throws IOException {
        Date startTime = new Date();
        final HeadlessWorkflowExecutionVerificationRecorder wfVerificationResultRecorder;
        wfVerificationResultRecorder =
            HeadlessWorkflowExecutionVerification.createAndInitializeInstance(workflowRootFile, wfFilesExpectedToSucceed,
                wfFilesExpectedToFail, parallelRuns, sequentialRuns);
        final Collection<File> wfFiles =
            Stream.concat(wfFilesExpectedToSucceed.stream(), wfFilesExpectedToFail.stream()).collect(Collectors.toList());
        for (int j = 0; j < sequentialRuns; j++) {
            Set<HeadlessWorkflowExecutionContext> headlessWfExeContexts = new HashSet<>();
            for (int i = 0; i < parallelRuns; i++) {
                for (final File wfFile : wfFiles) {
                    if (workflowVerificationService.preValidateWorkflow(outputReceiver, wfFile, false)) { // false = do not print
                                                                                                                // pre-verification output
                        try {
                            // TODO specify log directory?
                            HeadlessWorkflowExecutionContextBuilder exeContextBuilder =
                                new HeadlessWorkflowExecutionContextBuilder(wfFile);
                            exeContextBuilder.setLogDirectory(logfolderFactory.constructLogFolderForWorkflowFile(wfFile));
                            exeContextBuilder.setPlaceholdersFile(placeholdersFile);
                            exeContextBuilder
                                .setTextOutputReceiver(new PrefixingTextOutForwarder("[workflow execution] ", outputReceiver));
                            exeContextBuilder.setDisposalBehavior(dispose);
                            exeContextBuilder.setDeletionBehavior(delete);
                            headlessWfExeContexts.add(exeContextBuilder.build());
                        } catch (IOException | InvalidFilenameException e) {
                            wfVerificationResultRecorder.addWorkflowError(wfFile, e.getMessage());
                        }
                    }
                }
            }
            workflowExecutionService.executeWorkflowsAndVerify(headlessWfExeContexts, wfVerificationResultRecorder);
        }
        wfVerificationResultRecorder.setStartAndEndTime(startTime, new Date());
        return WorkflowVerificationResults
            .fromHeadlessWorkflowExecutionVerificationResult((HeadlessWorkflowExecutionVerificationResult) wfVerificationResultRecorder);
    }
}

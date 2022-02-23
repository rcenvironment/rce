/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.extras.testscriptrunner.internal;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.WorkflowStepDefinitions;

/**
 * Wrapper around a managed instance that adds some methods to simplify its use in
 * {@link WorkflowStepDefinitions#thenThatWorkflowRunShouldBeIdenticalTo(String)}.
 * 
 * @author Alexander Weinert
 */
public class GildedManagedInstance {

    private final ManagedInstance instance;

    private final Consumer<String> commandConsolePrinter;

    private final Function<String, String> executeActionOnInstance;

    public GildedManagedInstance(ManagedInstance instance, Consumer<String> commandConsolePrinter,
        BiFunction<ManagedInstance, String, String> executeActionOnInstance) {
        this.instance = instance;
        this.commandConsolePrinter = commandConsolePrinter;
        this.executeActionOnInstance = command -> executeActionOnInstance.apply(this.instance, command);
    }

    private void printToCommandConsole(String message) {
        this.commandConsolePrinter.accept(message);
    }

    private String executeCommandOnInstance(String action) {
        return this.executeActionOnInstance.apply(action);
    }

    public String exportWorkflowRun(File tmpDirInst, String workflowRun) {
        String outputExportInst = this.executeCommandOnInstance(StringUtils.format("tc export_wf_run %s %s", tmpDirInst, workflowRun));
        this.printToCommandConsole(outputExportInst);
        if (!outputExportInst.contains(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_EXPORT)) {
            fail(StringUtils.format("The workflow run %s could not be exported from instance %s", workflowRun, instance));
        }
        return StringUtils.format("%s\\%s.json", tmpDirInst, workflowRun.replace(":", "_"));
    }

    public boolean compareWorkflowRuns(final String masterPath, final String exportedRunPath) {
        String outputComparison =
            this.executeCommandOnInstance(StringUtils.format("tc compare_wf_runs %s %s", masterPath, exportedRunPath));
        printToCommandConsole(outputComparison);
        return outputComparison.contains(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_COMPARISON_IDENTICAL);
    }

    public Iterable<String> findWorkflowRunsByPattern(String workflowName) {
        String commandOutput = executeCommandOnInstance("wf");

        Pattern p = Pattern.compile("'(" + workflowName + "[\\d-_:]+)'");
        Matcher m = p.matcher(commandOutput);
        final List<String> returnValue = new LinkedList<>();
        while (m.find()) {
            returnValue.add(m.group(1));
        }
        return returnValue;
    }
}

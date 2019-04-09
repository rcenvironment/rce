/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.extras.testscriptrunner.definitions.common.RceTestLifeCycleHooks;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.InstanceManagementStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.WorkflowStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.internal.CucumberTestFrameworkAdapter;
import de.rcenvironment.extras.testscriptrunner.internal.CucumberTestFrameworkAdapter.ExecutionResult;

/**
 * {@link CommandPlugin} for executing BDD test scripts via the "run-test" command.
 *
 * @author Robert Mischke
 */
public class TestScriptRunnerCommandPlugin implements CommandPlugin {

    private static final String USAGE_INFO_PARAMETER_PART = "<comma-separated test ids>|--all <build id>";

    private static final String SEPARATOR_TEXT_LINE =
        "-----------------------------------------------------------------------------------------------";

    private ConfigurationSegment configuration;

    private File scriptLocationRoot;

    private final CucumberTestFrameworkAdapter testFrameworkAdapter;

    private final Log log = LogFactory.getLog(getClass());

    private final File reportsRootDir;

    public TestScriptRunnerCommandPlugin() throws IOException {
        testFrameworkAdapter = new CucumberTestFrameworkAdapter(
            RceTestLifeCycleHooks.class,
            InstanceManagementStepDefinitions.class,
            WorkflowStepDefinitions.class);
        reportsRootDir = TempFileServiceAccess.getInstance().createManagedTempDir("tsr_reports");
    }

    protected void bindConfigurationService(ConfigurationService configurationService) {
        configuration = configurationService.getConfigurationSegment("testScriptRunner");
        String scriptLocation = configuration.getString("scriptLocation");
        if (!StringUtils.isNullorEmpty(scriptLocation)) {
            scriptLocationRoot = new File(scriptLocation);
        } else {
            // no explicit setting found - try to use the default scripts bundle location
            try {
                scriptLocationRoot = configurationService.getUnpackedFilesLocation("testScripts");
            } catch (ConfigurationException e) {
                log.error("Failed to locate the default script file location, "
                    + "and no explicit path setting found - disabling TestScriptRunner",
                    e);
                scriptLocationRoot = null;
                return;
            }
        }
        log.debug("Using test script folder " + scriptLocationRoot);
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        if (scriptLocationRoot == null) {
            throw CommandException.executionError("The run-test feature is disabled because it was not correctly configured", context);
        }
        String mainCommand = context.consumeNextToken();
        if (!"run-test".equals(mainCommand) && !"run-tests".equals(mainCommand)) {
            throw CommandException.unknownCommand(context);
        }
        try {
            performRunTests(context);
        } catch (IOException e) {
            throw CommandException.executionError(e.getMessage(), context);
        }
    }

    private void performRunTests(CommandContext context) throws IOException, CommandException {

        if (context.getOriginalTokens().size() != 3) {
            throw CommandException.syntaxError(
                "Wrong number of parameters\n"
                    + "  Usage: run-test[s] " + USAGE_INFO_PARAMETER_PART + "\n"
                    + "  Example: run-test Test03 snapshots/trunk",
                context);
        }
        String tagNameFilter = context.consumeNextToken();
        if ("--all".equals(tagNameFilter)) {
            tagNameFilter = null; // execute all
        }

        String buildUnderTestId = context.consumeNextToken();

        final ExecutionResult result = testFrameworkAdapter.executeTestScripts(scriptLocationRoot, tagNameFilter,
            context.getOutputReceiver(), buildUnderTestId, reportsRootDir);

        List<String> reportLines = result.getReportFileLines();
        if (reportLines != null) {
            // dump generated text report to text console
            context.println("");
            context.println("Test run complete, content of report file:");
            context.println(SEPARATOR_TEXT_LINE);
            for (String line : reportLines) {
                context.println(line);
            }
            context.println(SEPARATOR_TEXT_LINE);
        } else {
            context.println("Test run complete (no report file found)");
        }

        List<String> stdOutLines = result.getCapturedStdOutLines();
        if (!stdOutLines.isEmpty()) {
            // dump generated text report to text console
            context.println("");
            context.println("Captured Output:");
            context.println(SEPARATOR_TEXT_LINE);
            for (String line : stdOutLines) {
                context.println(line);
            }
            context.println(SEPARATOR_TEXT_LINE);
        }
    }

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        ArrayList<CommandDescription> result = new ArrayList<CommandDescription>();
        result.add(new CommandDescription("run-test", USAGE_INFO_PARAMETER_PART, true, null));
        result.add(new CommandDescription("run-tests", "", true, "(alias of \"run-test\")"));
        return result;
    }

}

/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.ListCommandParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.MultiStateParameter;
import de.rcenvironment.core.command.spi.NamedParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedListParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.extras.testscriptrunner.definitions.common.RceTestLifeCycleHooks;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.AssertOutputStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.CommonStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.ComponentStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.InstanceCommandStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.InstanceInstantiationStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.InstanceNetworkingStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.InstanceStateStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.WorkflowStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.internal.CucumberTestFrameworkAdapter;
import de.rcenvironment.extras.testscriptrunner.internal.CucumberTestFrameworkAdapter.ExecutionResult;
import de.rcenvironment.extras.testscriptrunner.internal.CucumberTestFrameworkAdapter.ReportOutputFormat;

/**
 * {@link CommandPlugin} for executing BDD test scripts via the "run-test"
 * command.
 *
 * @author Robert Mischke
 * @author Marlon Schroeter (minor alterations)
 */
public class TestScriptRunnerCommandPlugin implements CommandPlugin {

    private static final String SEPARATOR_TEXT_LINE =
            "-----------------------------------------------------------------------------------------------";

    private static final String FORMAT = "--format";
    
    private static final String PRETTY = "pretty";
    
    private static final String JSON = "json";
    
    private static final String ALL_TOKEN = ":all";
    
    private static final StringParameter BUILD_UNDER_TEST_ID_PARAMETER = new StringParameter(null,
            "build under test id", "build version to be used");
    
    private static final StringParameter TAG_NAME_FILTER_PARAMETER = new StringParameter(null,
            "tag name filter", "filter for tag names");
    
    private static final ListCommandParameter LIST_TAG_NAME_FILTER_PARAMETER = new ListCommandParameter(TAG_NAME_FILTER_PARAMETER,
            "tag name filters", "filter for tag names");
    
    private static final MultiStateParameter FORMAT_PARAMETER = new MultiStateParameter("format", "output format", PRETTY, JSON);
    
    private static final NamedSingleParameter NAMED_FORMAT_PARAMETER = new NamedSingleParameter(FORMAT, "output format", FORMAT_PARAMETER);
    
    private ConfigurationSegment configuration;

    private File scriptLocationRoot;

    private final CucumberTestFrameworkAdapter testFrameworkAdapter;

    private final Log log = LogFactory.getLog(getClass());

    private final File reportsRootDir;

    public TestScriptRunnerCommandPlugin() throws IOException {
        testFrameworkAdapter = new CucumberTestFrameworkAdapter(AssertOutputStepDefinitions.class,
                CommonStepDefinitions.class, ComponentStepDefinitions.class, InstanceCommandStepDefinitions.class,
                InstanceInstantiationStepDefinitions.class, InstanceNetworkingStepDefinitions.class,
                InstanceStateStepDefinitions.class, RceTestLifeCycleHooks.class, WorkflowStepDefinitions.class);
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // avoid breaking when activated in a default test environment
            TempFileServiceAccess.setupUnitTestEnvironment();
        }
        reportsRootDir = TempFileServiceAccess.getInstance().createManagedTempDir("tsr_reports");
    }

    protected void bindConfigurationService(ConfigurationService configurationService) {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // skip implicit bind actions if is was spawned as part of a default test
            // environment;
            // if this causes errors in mocked service tests, invoke
            // RuntimeDetection.allowSimulatedServiceActivation()
            return;
        }

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
                        + "and no explicit path setting found - disabling TestScriptRunner", e);
                scriptLocationRoot = null;
                return;
            }
        }
        log.debug("Using test script folder " + scriptLocationRoot);
    }

    private void performRunTests(CommandContext context) throws IOException, CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter buildUnderTestIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        String buildUnderTestId = buildUnderTestIdParameter.getResult();
        
        final CucumberTestFrameworkAdapter.ReportOutputFormat reportFormat = extractReportFormat(context);
        final String tagNameFilter = extractTagNameFilter(modifiers);

        final ExecutionResult result = testFrameworkAdapter.executeTestScripts(scriptLocationRoot, tagNameFilter,
                context.getOutputReceiver(), buildUnderTestId, reportsRootDir, reportFormat);

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

    private String extractTagNameFilter(ParsedCommandModifiers modifiers) {
        ParsedListParameter tagNameFilterParameter = (ParsedListParameter) modifiers.getPositionalCommandParameter(0);

        StringJoiner joiner = new StringJoiner(",");
        
        for (int i = 0; i < tagNameFilterParameter.getResult().size(); i++) {
            ParsedStringParameter parameter = (ParsedStringParameter) tagNameFilterParameter.getResult().get(i);
            
            if (parameter.getResult().equals(ALL_TOKEN)) {
                return null;
            }
            
            joiner.add(parameter.getResult());
        }
        
        return joiner.toString();
    }

    private CucumberTestFrameworkAdapter.ReportOutputFormat extractReportFormat(CommandContext context)
            throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();

        ParsedStringParameter formatParameter = (ParsedStringParameter) modifiers.getCommandParameter(FORMAT);

        /*
         * Here we manually match user input to supported output formats. Judging by
         * lines of code, it would be easier to do something like
         * `ReportOutputFormat.values().filter(val ->
         * val.getFormatSpecifier().equals(formatSpecifier)).findAny()`. This, however,
         * would tie our user interface on the RCE side directly to the plugin
         * specification on the Cucumber side. Currently, the two are aligned, e.g., RCE
         * users specify "pretty" if they want to use the Cucumber-output-plugin
         * "pretty". In the future, however, the two may diverge.
         */
        switch (formatParameter.getResult()) {
        case PRETTY:
            return ReportOutputFormat.PRETTY;
        case JSON:
            return ReportOutputFormat.JSON;
        default:
            throw CommandException.syntaxError(
                    StringUtils.format("Unknown report format specifier '%s'. Supported report formats: pretty, json",
                            formatParameter.getResult()),
                    context);
        }
    }

    @Override
    public MainCommandDescription[] getCommands() {
        return new MainCommandDescription[] {
            new MainCommandDescription("run-test", "run a test", "run a test", context -> {
                try {
                    performRunTests(context);
                } catch (IOException e) {
                    throw CommandException.executionError(e.getMessage(), context);
                }
            }, new CommandModifierInfo(
                new AbstractCommandParameter[] {
                    LIST_TAG_NAME_FILTER_PARAMETER,
                    BUILD_UNDER_TEST_ID_PARAMETER
                },
                new NamedParameter[] {
                    NAMED_FORMAT_PARAMETER
                }
            ), true),
            new MainCommandDescription("run-tests", "(alias of \"run-test\")", "(alias of \"run-test\")",
                context -> {
                    try {
                        performRunTests(context);
                    } catch (IOException e) {
                        throw CommandException.executionError(e.getMessage(), context);
                    }
                },
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        LIST_TAG_NAME_FILTER_PARAMETER,
                        BUILD_UNDER_TEST_ID_PARAMETER
                    },
                    new NamedParameter[] {
                        NAMED_FORMAT_PARAMETER
                    }
                ), true)
        };
    }

}

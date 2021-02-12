/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cucumber.api.Pending;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.ObjectFactory;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.Backend;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.ClasspathResourceLoader;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.picocontainer.PicoFactory;
import cucumber.runtime.model.CucumberFeature;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;

/**
 * A wrapper around the Cucumber BDD test framework to encapsulate various setup, classloader and file location issues. It is intended to
 * make test scripts runnable from plain (non-OSGi) unit tests, a RCE command plugin embedded in an RCE launch from Eclipse, and the same
 * command plugin when installed into a standalone RCE product.
 *
 * @author Robert Mischke
 */
public class CucumberTestFrameworkAdapter {

    private static final String CLI_OPTION_PLUGIN = "-p";

    private static final String CLI_OPTION_SNIPPETS = "--snippets";

    private static final String CLI_OPTION_MONOCHROME = "-m";

    private static final String CLI_OPTION_STRICT_MODE = "-s";

    private static final String CLI_OPTION_GLUE_CODE = "-g";

    private static final String CLI_OPTION_TAG_FILTER = "-t";

    private final Backend javaBackend;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Simple container for execution result data.
     *
     * @author Robert Mischke
     */
    public static final class ExecutionResult {

        private List<String> reportFileLines;

        private List<String> capturedStdOutLines;

        public ExecutionResult(List<String> reportLines, List<String> capturedStdOutLines) {
            this.reportFileLines = reportLines;
            this.capturedStdOutLines = capturedStdOutLines;
        }

        public List<String> getReportFileLines() {
            return reportFileLines;
        }

        public List<String> getCapturedStdOutLines() {
            return capturedStdOutLines;
        }

    }

    public CucumberTestFrameworkAdapter(final Class<?>... stepDefinitions) {
        // TODO document rationale behind this
        ClassLoader classLoader = getClass().getClassLoader();
        ClassFinder patchedClassFinder = new ResourceLoaderClassFinder(new ClasspathResourceLoader(classLoader), classLoader) {

            @Override
            @SuppressWarnings("unchecked")
            public <T> Collection<Class<? extends T>> getDescendants(Class<T> parentType, String packageName) {
                List<Class<? extends T>> result = new ArrayList<>();
                if (parentType == Object.class && packageName.endsWith(".definitions")) {
                    log.debug("Injecting BDD step definitions...");
                    for (Class<?> definitionClass : stepDefinitions) {
                        result.add((Class<? extends T>) definitionClass);
                    }
                } else if (parentType == Annotation.class && packageName.equals("cucumber.api")) {
                    log.debug("Injecting BDD framework annotations...");
                    result.add((Class<? extends T>) Given.class);
                    result.add((Class<? extends T>) When.class);
                    result.add((Class<? extends T>) Then.class);
                    result.add((Class<? extends T>) Pending.class);
                    result.add((Class<? extends T>) Before.class);
                    result.add((Class<? extends T>) After.class);
                } else {
                    // ignore the know scan for Java 8 step definitions, but anything else would be suspicious -- misc_ro
                    if (parentType != cucumber.api.java8.GlueBase.class) {
                        log.warn("Unexpected subtype request from BDD framework: " + parentType + " / " + packageName);
                    }
                }
                return result;
            }
        };
        ObjectFactory factory = new PicoFactory();
        factory.addClass(TestScenarioExecutionContext.class);
        javaBackend = new JavaBackend(factory, patchedClassFinder);
    }

    /**
     * Executes a number of test scenarios loaded from "feature" files (see Cucumber docs for terminology).
     * 
     * @param scriptLocationRoot the directory that is scanned for .feature files
     * @param tagNameSelection a comma-separated list of tags to include in the test run (joined by logical OR); an empty or null list
     *        executes all tests
     * @param outputReceiver the {@link TextOutputReceiver} for test script output
     * @param buildUnderTestId the id of the build to test; injected into the {@link TestScenarioExecutionContext} for consumption by step
     *        definitions
     * @param reportDir the directory to write the generated reports to
     * @return an {@link ExecutionResult} containing: 1. the generated report file's lines, or null if no report was generated; 2. the
     *         captured StdOut lines (never null, but could in theory be empty)
     * @throws IOException on I/O errors
     */
    public ExecutionResult executeTestScripts(File scriptLocationRoot, String tagNameSelection, TextOutputReceiver outputReceiver,
        String buildUnderTestId, File reportDir) throws IOException {

        // TODO (p2) check whether this can be reworked to use an individual file per run; this would enable parallel runs
        final String reportDirUriString = reportDir.toURI().toASCIIString();
        File reportFile = new File(reportDir, "plain.txt");
        if (reportFile.isFile()) {
            reportFile.delete(); // ignore return value; result is checked below, along with potential existence as directory
        }
        if (reportFile.isFile()) {
            throw new IOException("Failed to delete pre-existing report file " + reportFile.getAbsolutePath());
        }

        List<String> cliParts = new ArrayList<>();

        cliParts.add(CLI_OPTION_TAG_FILTER);
        cliParts.add("~@disabled"); // exclude tests tagged with @disabled by default (case sensitive)

        cliParts.add(CLI_OPTION_TAG_FILTER);
        cliParts.add("~@Disabled"); // exclude tests tagged with @Disabled by default (case sensitive)

        if (!StringUtils.isNullorEmpty(tagNameSelection)) {
            // normalize filter parts and prepend "@" character
            final StringBuilder buffer = new StringBuilder();
            for (String filterPart : tagNameSelection.split(",")) {
                if (buffer.length() != 0) {
                    buffer.append(",");
                }
                final String trimmedPart = filterPart.trim();
                if (!trimmedPart.startsWith("@")) {
                    buffer.append("@");
                }
                buffer.append(trimmedPart);
            }
            if (buffer.length() != 0) {
                cliParts.add(CLI_OPTION_TAG_FILTER);
                cliParts.add(buffer.toString());
            }
        }

        // see https://cucumber.io/docs/reference/jvm#cli-runner for options reference
        cliParts.addAll(Arrays.asList(new String[] {
            CLI_OPTION_GLUE_CODE, "de.rcenvironment.extras.testscriptrunner.definitions", // register definitions
            CLI_OPTION_STRICT_MODE, // strict mode: treat undefined and pending steps as errors
            CLI_OPTION_MONOCHROME, // monochrome output
            CLI_OPTION_SNIPPETS, "camelcase", // use CamelCase in snippets (instead of underscore separators)
            CLI_OPTION_PLUGIN, "pretty:" + reportDirUriString + "/plain.txt", // add and configure plain text report
            // "-p", "html:" + reportDirUriString + "/html", // add and configure html report
            scriptLocationRoot.getAbsolutePath() // define the location of test script files ("features")
        }));

        RuntimeOptions runtimeOptions = new RuntimeOptions(cliParts) {

            @Override
            // overridden to inject a FileResourceLoader; otherwise, no script files are found when running as a plugin -- misc_ro
            public List<CucumberFeature> cucumberFeatures(ResourceLoader resourceLoader) {
                List<CucumberFeature> result =
                    CucumberFeature.load(new FileResourceLoader(), getFeaturePaths(), getFilters(), System.out);
                return result;
            }
        };

        ClassLoader classLoader = getClass().getClassLoader(); // the bundle classloader
        ResourceLoader resourceLoader = new ClasspathResourceLoader(classLoader);
        ArrayList<Backend> backends = new ArrayList<Backend>();
        backends.add(javaBackend);

        Runtime runtime = new Runtime(resourceLoader, classLoader, backends, runtimeOptions);

        PrintStream oldStdOut = System.out;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        final PrintStream outputWriter = new PrintStream(outputBuffer, false, "UTF-8");
        System.setOut(outputWriter);

        TestScenarioExecutionContext.setThreadLocalParameters(outputReceiver, buildUnderTestId, scriptLocationRoot);
        try {
            runtime.run();
        } finally {
            TestScenarioExecutionContext.discardThreadLocalParameters();
        }

        System.setOut(oldStdOut);
        outputWriter.close();

        if (reportFile.isFile()) {
            final List<String> reportLines = FileUtils.readLines(reportFile, Charsets.UTF_8); // TODO charset correct?
            final List<String> capturedStdOutLines = IOUtils.readLines(new ByteArrayInputStream(outputBuffer.toByteArray()), "UTF-8");
            return new ExecutionResult(reportLines, capturedStdOutLines);
        } else {
            return null;
        }
    }
}

/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.validator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.components.script.execution.Messages;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;
import de.rcenvironment.core.scripting.python.PythonComponentConstants;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Validator for script component.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel
 * @author Martin Misiak
 * @author David Scholz
 * @author Doreen Seider (caching)
 */
public class ScriptComponentValidator extends AbstractComponentValidator {

    private static final String PYTHON_VALIDATION_ERROR = "Validation of python path failed.";

    private static final Long PYTHON_TEST_TIMEOUT = 1L;

    private static final int MINUS_ONE = -1;

    private static final String COLON = ": ";

    private static Log logger = LogFactory.getLog(ScriptComponentValidator.class);

    private ExecutionOutputCache executionOutputCache = createExecutionOutputCache();

    @Override
    public String getIdentifier() {
        return ScriptComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {

        final List<ComponentValidationMessage> messages = new ArrayList<>();

        String script = getProperty(componentDescription, SshExecutorConstants.CONFIG_KEY_SCRIPT);
        if (script == null || script.isEmpty()) {
            final ComponentValidationMessage noScriptMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, SshExecutorConstants.CONFIG_KEY_SCRIPT, Messages.noScript,
                Messages.noScript + " defined");
            messages.add(noScriptMessage);
        } else if (script.endsWith(ScriptComponentConstants.DEFAULT_SCRIPT_LAST_LINE)) {
            final ComponentValidationMessage defaultScriptMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.WARNING, SshExecutorConstants.CONFIG_KEY_SCRIPT,
                Messages.defaultScriptMessage,
                Messages.defaultScriptMessage);
            messages.add(defaultScriptMessage);
        } else if (!checkScriptIndentationConsistency(script)) {
            final ComponentValidationMessage inconsistentScriptMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.WARNING, SshExecutorConstants.CONFIG_KEY_SCRIPT,
                Messages.scriptInconsistentIndentation,
                Messages.scriptInconsistentIndentation + COLON + SshExecutorConstants.CONFIG_KEY_SCRIPT);
            messages.add(inconsistentScriptMessage);
        }

        return messages;
    }

    /**
     * @param script String containing the script
     * @return <code>true</code> if either whitespace or tabs are exclusively used for the indentation of a script; <code>false</code>
     *         otherwise.
     */
    private boolean checkScriptIndentationConsistency(String script) {

        String regexWs = "^ +([\\S].*)$";
        String regexWsOnly = "^( +)$";
        String regexTab = "^\\t+([\\S].*)$";
        String regexTabOnly = "^(\\t+)$";
        String eol = System.getProperty("line.separator");
        String[] scriptLines = script.split(eol);
        boolean ws = false;
        boolean tab = false;

        for (int i = 0; i < scriptLines.length; i++) {
            if (!ws) {
                ws = scriptLines[i].matches(regexWs) || scriptLines[i].matches(regexWsOnly);
            }
            if (!tab) {
                tab = scriptLines[i].matches(regexTab) || scriptLines[i].matches(regexTabOnly);
            }
            if (scriptLines[i].matches("^(( +\\t+)|(\\t+ +)).*")) {
                return false;
            }
        }
        if (ws && tab) {
            return false;
        }

        return true;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        return null;
    }

    @Override
    public List<ComponentValidationMessage> validateOnWorkflowStart(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<>();
        if (getProperty(componentDescription, ScriptComponentConstants.SCRIPT_LANGUAGE).equals("Python")) {
            String pythonInstallation = getProperty(componentDescription, PythonComponentConstants.PYTHON_INSTALLATION);

            if (!pythonInstallation.isEmpty()) {
                final PythonVersionRegexValidator validator = new PythonVersionRegexValidator();
                if (executionOutputCache.containsKey(pythonInstallation)) {
                    replayValidationWithCachedExecutionOutput(pythonInstallation, validator);
                } else {
                    executeAndValidate(pythonInstallation, validator);
                }
                processPythonValidationResult(validator, messages, pythonInstallation);
            }
        }

        return messages;
    }

    private void replayValidationWithCachedExecutionOutput(final String pythonInstallation, final PythonVersionRegexValidator validator) {
        for (String output : executionOutputCache.get(pythonInstallation)) {
            validator.validatePythonVersion(output);
        }
    }

    private void executeAndValidate(final String pythonInstallation, final PythonVersionRegexValidator validator) {
        final LocalApacheCommandLineExecutor executor;

        final TextStreamWatcher stdOutTextStreamWatcher;
        final TextStreamWatcher stdErrTextStreamWatcher;
        String command = "\"" + pythonInstallation + "\"" + " --version";

        try {

            executor = createCommandLineExecutor(new File("/"));
            executor.start(command);
            stdOutTextStreamWatcher = new TextStreamWatcher(executor.getStdout(),
                ConcurrencyUtils.getAsyncTaskService(), new CapturingTextOutReceiver() {

                    @Override
                    public synchronized void addOutput(String line) {
                        super.addOutput(line);
                        validator.validatePythonVersion(getBufferedOutput());
                    }

                });
            stdErrTextStreamWatcher =
                new TextStreamWatcher(executor.getStderr(), ConcurrencyUtils.getAsyncTaskService(),
                    new CapturingTextOutReceiver() {

                        @Override
                        public synchronized void addOutput(String line) {
                            super.addOutput(line);
                            validator.validatePythonVersion(getBufferedOutput());
                        }

                    });

            stdOutTextStreamWatcher.start();
            stdErrTextStreamWatcher.start();

            final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();
            Future<?> task = asyncTaskService.submit(new Runnable() {

                @TaskDescription("Waits for python validation to finish")
                @Override
                public void run() {
                    try {
                        executor.waitForTermination();
                        stdOutTextStreamWatcher.waitForTermination();
                        stdErrTextStreamWatcher.waitForTermination();
                    } catch (IOException e) {
                        logger.error(PYTHON_VALIDATION_ERROR, e);
                    } catch (InterruptedException e) {
                        logger.error(PYTHON_VALIDATION_ERROR, e);
                    }
                }
            });

            // TODO review exception handling: why is executor.cancel() called in any case? and in case of a TimeoutException it is actually
            // called twice. TimeoutException should be handled in some way, at least logged --seid_do
            try {
                task.get(PYTHON_TEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                executor.cancel();
            } catch (ExecutionException e) {
                logger.error(PYTHON_VALIDATION_ERROR, e);
            } finally {
                executor.cancel();
            }

            executionOutputCache.put(pythonInstallation, validator.getOutputValidated());

        } catch (IOException | InterruptedException e) {
            logger.error(PYTHON_VALIDATION_ERROR, e);
        }
    }

    private void processPythonValidationResult(PythonVersionRegexValidator validator,
        List<ComponentValidationMessage> messages, String path) {
        if (!validator.isPythonExecutionSuccessful()
            && validator.getMajorPythonVersion() == MINUS_ONE) {
            final ComponentValidationMessage message = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, PythonComponentConstants.PYTHON_INSTALLATION,
                Messages.pythonExecutionTestErrorRelative, StringUtils.format(Messages.pythonExecutionTestErrorRelative, path));
            messages.add(message);
        } else if (!validator.isPythonExecutionSuccessful() && (validator.getMinorPythonVersion() < 6
            && validator.getMajorPythonVersion() >= 2)) {
            final ComponentValidationMessage message = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, PythonComponentConstants.PYTHON_INSTALLATION,
                Messages.pythonExecutionUnsupportedVersionRelative,
                Messages.pythonExecutionUnsupportedVersionRelative);
            messages.add(message);
        } else if (validator.isPythonExecutionSuccessful()) {
            logPythonExecutionSuccessfulMessage(validator);
        }
    }

    private void logPythonExecutionSuccessfulMessage(PythonVersionRegexValidator validator) {
        LogFactory.getLog(this.getClass())
            .debug("Python Version Used: " + validator.getMajorPythonVersion() + "."
                + validator.getMinorPythonVersion() + "."
                + validator.getMicroPythonVersion());
    }

    /**
     * @return the {@link PythonVersionRegexValidator}. Intended for unit testing.
     */
    protected PythonVersionRegexValidator createPythonVersionRegexValidator() {
        return new PythonVersionRegexValidator();
    }

    /**
     * @return the {@link LocalApacheCommandLineExecutor} to use. Intended for unit testing.
     */
    protected LocalApacheCommandLineExecutor createCommandLineExecutor(File workDirPath) throws IOException {
        return new LocalApacheCommandLineExecutor(workDirPath);
    }

    /**
     * @return the {@link ExecutionOutputCache} to use. Intended for unit testing.
     */
    protected ExecutionOutputCache createExecutionOutputCache() {
        return new ExecutionOutputCache();
    }

    /**
     * Caches the execution output produced when executing the given Python installation path.
     * 
     * @author Doreen Seider
     */
    protected static class ExecutionOutputCache {

        private static final int CACHE_EVICTION_TIME_MILLIS = 3000;

        private PassiveExpiringMap<String, List<String>> cachedExecutionOutput = new PassiveExpiringMap<>(CACHE_EVICTION_TIME_MILLIS);

        protected void put(String key, List<String> output) {
            cachedExecutionOutput.put(key, output);
        }

        protected List<String> get(String key) {
            return cachedExecutionOutput.get(key);
        }

        protected boolean containsKey(String key) {
            return cachedExecutionOutput.containsKey(key);
        }

    }

    // TODO add JavaDoc --seid_do
    /**
     * 
     * @author David Scholz
     *
     */
    protected static class PythonVersionRegexValidator {

        private static final int MINUS_ONE = -1;

        private static final Pattern MATCH_PATTERN = Pattern.compile("^Python\\s([0-9]+)\\.([0-9]+)\\.([0-9]+)");

        private volatile int majorPythonVersion = MINUS_ONE;

        private volatile int minorPythonVersion = MINUS_ONE;

        private volatile int microPythonVersion = MINUS_ONE;

        private volatile boolean pythonExecutionSuccessful = false;

        private List<String> outputValidated = Collections.synchronizedList(new ArrayList<String>());

        /**
         * 
         * @param bufferedOutput stdout or stderr of the python exe.
         */
        public synchronized void validatePythonVersion(String bufferedOutput) {
            outputValidated.add(bufferedOutput);
            Matcher matcher = MATCH_PATTERN.matcher(bufferedOutput);
            if (matcher.find()) {
                majorPythonVersion = Integer.parseInt(matcher.group(1));
                minorPythonVersion = Integer.parseInt(matcher.group(2));
                microPythonVersion = Integer.parseInt(matcher.group(3));
            }

            if (majorPythonVersion == 3 || majorPythonVersion == 2 && minorPythonVersion >= 6) {
                pythonExecutionSuccessful = true;
            }
        }

        public boolean isPythonExecutionSuccessful() {
            return pythonExecutionSuccessful;
        }

        public List<String> getOutputValidated() {
            return outputValidated;
        }

        public int getMajorPythonVersion() {
            return this.majorPythonVersion;
        }

        public int getMinorPythonVersion() {
            return this.minorPythonVersion;
        }

        public int getMicroPythonVersion() {
            return this.microPythonVersion;
        }

    }

}

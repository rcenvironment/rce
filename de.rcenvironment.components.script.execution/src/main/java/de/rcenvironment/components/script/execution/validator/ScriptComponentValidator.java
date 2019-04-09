/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.validator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @author Doreen Seider (caching, first implementation)
 * @author Thorsten Sommer (fixed version parsing + caching, second implementation)
 */
public class ScriptComponentValidator extends AbstractComponentValidator {

    private static final String PYTHON_VALIDATION_ERROR = "Validation of python path failed.";

    private static final Long PYTHON_TEST_TIMEOUT = 1L;

    private static final int MINUS_ONE = -1;

    private static final String COLON = ": ";

    private static Log logger = LogFactory.getLog(ScriptComponentValidator.class);

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
            final String pythonInstallation = getProperty(componentDescription, PythonComponentConstants.PYTHON_INSTALLATION);
            if (!pythonInstallation.isEmpty()) {
                final PythonValidationResult cachedResult =
                    ScriptComponentValidatorCache.getCache().getValidationResult(pythonInstallation);
                if (!cachedResult.isPlaceholder()) {
                    // A cached result gets used:
                    processPythonValidationResult(cachedResult, messages, pythonInstallation);
                } else {
                    // No cached result known. Validate this path:
                    final PythonVersionRegexValidator validator = new PythonVersionRegexValidator();
                    final PythonValidationResult result = executeAndValidate(pythonInstallation, validator);
                    processPythonValidationResult(result, messages, pythonInstallation);
                }
            }
        }

        return messages;
    }

    private PythonValidationResult executeAndValidate(final String pythonInstallation, final PythonVersionRegexValidator validator) {
        final LocalApacheCommandLineExecutor executor;
        final TextStreamWatcher stdOutTextStreamWatcher;
        final TextStreamWatcher stdErrTextStreamWatcher;
        final String command = "\"" + pythonInstallation + "\"" + " --version";

        try {

            executor = createCommandLineExecutor(new File("/"));
            executor.start(command);
            stdOutTextStreamWatcher = new TextStreamWatcher(executor.getStdout(),
                ConcurrencyUtils.getAsyncTaskService(), new CapturingTextOutReceiver() {

                    @Override
                    public synchronized void addOutput(String line) {
                        super.addOutput(line);
                        validator.validatePythonVersion(line, pythonInstallation);
                    }

                });
            stdErrTextStreamWatcher =
                new TextStreamWatcher(executor.getStderr(), ConcurrencyUtils.getAsyncTaskService(),
                    new CapturingTextOutReceiver() {

                        @Override
                        public synchronized void addOutput(String line) {
                            super.addOutput(line);
                            validator.validatePythonVersion(line, pythonInstallation);
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

            try {
                // Wait at most the desired amount of time to finish:
                task.get(PYTHON_TEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                // Execution took too long:
                executor.cancel();
            } catch (ExecutionException e) {
                logger.error(PYTHON_VALIDATION_ERROR, e);
            } finally {
                // Cancel the process in cases where an execution exception or an arbitrary other exception was thrown:
                executor.cancel();
            }

            // Add or update the cache:
            final PythonValidationResult result = validator.getValidationResult();
            ScriptComponentValidatorCache.getCache().addOrUpdateValidationResult(result);
            return result;

        } catch (IOException | InterruptedException e) {
            logger.error(PYTHON_VALIDATION_ERROR, e);
        }

        return PythonValidationResult.DEFAULT_NONE_PLACEHOLDER;
    }

    private void processPythonValidationResult(final PythonValidationResult validatorResult,
        final List<ComponentValidationMessage> messages, final String path) {
        if (!validatorResult.isPythonExecutionSuccessful() && validatorResult.getMajorPythonVersion() == MINUS_ONE) {
            final ComponentValidationMessage message = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, PythonComponentConstants.PYTHON_INSTALLATION,
                Messages.pythonExecutionTestErrorRelative, StringUtils.format(Messages.pythonExecutionTestErrorRelative, path));
            messages.add(message);
        } else if (!validatorResult.isPythonExecutionSuccessful()
            && (validatorResult.getMinorPythonVersion() < 6 && validatorResult.getMajorPythonVersion() >= 2)) {
            final ComponentValidationMessage message = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, PythonComponentConstants.PYTHON_INSTALLATION,
                Messages.pythonExecutionUnsupportedVersionRelative,
                Messages.pythonExecutionUnsupportedVersionRelative);
            messages.add(message);
        } else if (validatorResult.isPythonExecutionSuccessful()) {
            logPythonExecutionSuccessfulMessage(validatorResult);
        }
    }

    private void logPythonExecutionSuccessfulMessage(final PythonValidationResult validatorResult) {
        LogFactory.getLog(this.getClass())
            .debug("Python Version Used: " + validatorResult.getMajorPythonVersion() + "."
                + validatorResult.getMinorPythonVersion() + "."
                + validatorResult.getMicroPythonVersion());
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
     * 
     * The Python validation result class. This is a container for the recognized Python version as well as its path. As placeholder for not
     * yet known values, the static {@link DEFAULT_NONE_PLACEHOLDER} might be used.
     *
     * @author Thorsten Sommer
     */
    protected static final class PythonValidationResult {

        /**
         * A static placeholder value which might be used instead of null for not yet known values.
         * 
         */
        public static final PythonValidationResult DEFAULT_NONE_PLACEHOLDER = new PythonValidationResult("/none", -1, -1, -1, false);

        private final int majorPythonVersion;

        private final int minorPythonVersion;

        private final int microPythonVersion;

        private final boolean successfull;

        private final String pythonPath;

        /**
         * 
         * This is the constructor for a Python validation result.
         * 
         * @param pythonPath The validated Python path. It cannot null nor an empty string.
         * @param majorVersion Python's major version
         * @param minorVersion Python's minor version
         * @param microVersion Python's micro version
         * @param successfull The final state of the validation process. True means, that the Python version was accepted.
         */
        public PythonValidationResult(final String pythonPath, final int majorVersion, final int minorVersion, final int microVersion,
            final boolean successfull) {
            if (pythonPath == null) {
                throw new NullPointerException("The given Python path cannot be null");
            }
            
            if (pythonPath.isEmpty()) {
                throw new IllegalArgumentException("The given Python path cannot be empty.");
            }

            this.pythonPath = pythonPath;
            this.majorPythonVersion = majorVersion;
            this.minorPythonVersion = minorVersion;
            this.microPythonVersion = microVersion;
            this.successfull = successfull;
        }

        /**
         * 
         * Returns true if the validation was successfully.
         * 
         * @return Returns true if the validation was successfully.
         */
        public boolean isPythonExecutionSuccessful() {
            if (this.isPlaceholder()) {
                return false;
            }

            return this.successfull;
        }

        /**
         * 
         * Returns Python's major version i.e. the first part of the version number.
         * 
         * @return Python's major version
         */
        public int getMajorPythonVersion() {
            return this.majorPythonVersion;
        }

        /**
         * 
         * Returns Python's minor version i.e. the second part of the version number.
         * 
         * @return Python's minor version
         */
        public int getMinorPythonVersion() {
            return this.minorPythonVersion;
        }

        /**
         * 
         * Returns Python's micro version i.e. the third part of the version number.
         * 
         * @return Python's micro version
         */
        public int getMicroPythonVersion() {
            return this.microPythonVersion;
        }

        /**
         * 
         * Returns Python's path which was used for the validation.
         * 
         * @return The Python path
         */
        public String getPythonPath() {
            return pythonPath;
        }

        /**
         * 
         * Return true if this instance is the placeholder.
         * 
         * @return True if this instance is the placeholder.
         */
        public boolean isPlaceholder() {
            return this == PythonValidationResult.DEFAULT_NONE_PLACEHOLDER;
        }
    }

    /**
     * 
     * This validator class validates Python installations. All methods are thread-safe for a particular instance.
     * 
     * @author David Scholz
     * @author Thorsten Sommer (refactored class)
     *
     */
    protected static final class PythonVersionRegexValidator {

        private static final Pattern MATCH_PATTERN = Pattern.compile("^Python\\s([0-9]+)\\.{0,}([0-9]+){0,}\\.{0,}([0-9]+){0,}");

        private static final int MINUS_ONE = -1;

        private PythonValidationResult currentResult = PythonValidationResult.DEFAULT_NONE_PLACEHOLDER;

        /**
         * This method validates one line from a python --version output. The method can handle multiple lines by processing them one after
         * another. Additional lines, after a Python version was recognized, are ignored.
         * 
         * @param line One line from stdout or stderr of a Python process.
         */
        public void validatePythonVersion(final String line, final String pythonPath) {

            if (this.currentResult.isPythonExecutionSuccessful()) {
                return;
            }
            
            int majorPythonVersion = MINUS_ONE;
            int minorPythonVersion = MINUS_ONE;
            int microPythonVersion = MINUS_ONE;

            Matcher matcher = MATCH_PATTERN.matcher(line);
            if (matcher.find()) {
                final String g1 = matcher.group(1);
                final String g2 = matcher.group(2);
                final String g3 = matcher.group(3);

                if (g1 != null) {
                    majorPythonVersion = Integer.parseInt(g1);
                }

                if (g2 != null) {
                    minorPythonVersion = Integer.parseInt(g2);
                }

                if (g3 != null) {
                    microPythonVersion = Integer.parseInt(g3);
                }
            }

            if (majorPythonVersion == 3 || majorPythonVersion == 2 && minorPythonVersion >= 6) {
                synchronized (this) {
                    this.currentResult =
                        new PythonValidationResult(pythonPath, majorPythonVersion, minorPythonVersion, microPythonVersion, true);
                    return;
                }
            }

            synchronized (this) {
                this.currentResult =
                    new PythonValidationResult(pythonPath, majorPythonVersion, minorPythonVersion, microPythonVersion, false);
            }
        }

        /**
         * 
         * Yields the current result as {@link PythonValidationResult}.
         * 
         * @return The current result as {@link PythonValidationResult}
         */
        public PythonValidationResult getValidationResult() {
            synchronized (this) {
                return this.currentResult;
            }
        }
    }
}

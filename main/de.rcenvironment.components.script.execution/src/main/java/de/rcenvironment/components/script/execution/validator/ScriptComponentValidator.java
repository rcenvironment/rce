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
 */
public class ScriptComponentValidator extends AbstractComponentValidator {

    private static final String PYTHON_VALIDATION_ERROR = "Validation of python path failed.";

    private static final Long PYTHON_TEST_TIMEOUT = 1L;

    private static final int MINUS_ONE = -1;

    private static final String COLON = ": ";

    private Log logger = LogFactory.getLog(ScriptComponentValidator.class);

    @Override
    public String getIdentifier() {
        return ScriptComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {

        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();

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
     * The method returns true if either whitespaces or tabs are exclusively used for the indentation of a script. False otherwise.
     * 
     * @param script String containing the script
     * @return
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ComponentValidationMessage> validateOnWorkflowStart(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();
        if (getProperty(componentDescription, ScriptComponentConstants.SCRIPT_LANGUAGE).equals("Python")) {
            String pythonInstallation = getProperty(componentDescription, PythonComponentConstants.PYTHON_INSTALLATION);

            if (!pythonInstallation.isEmpty()) {

                final LocalApacheCommandLineExecutor executor;

                TextStreamWatcher stdOutTextStreamWatcher;
                TextStreamWatcher stdErrTextStreamWatcher;
                final PythonVersionRegexValidator validator;
                String command = "\"" + pythonInstallation + "\"" + " --version";
                
                try {

                    validator = new PythonVersionRegexValidator();
                    executor = new LocalApacheCommandLineExecutor(new File("/"));
                    executor.start(command);
                    stdOutTextStreamWatcher = new TextStreamWatcher(executor.getStdout(),
                        ConcurrencyUtils.getAsyncTaskService(), new CapturingTextOutReceiver("") {

                            @Override
                            public synchronized void addOutput(String line) {
                                super.addOutput(line);
                                validator.validatePythonVersion(getBufferedOutput().toString());
                            }

                        });
                    stdErrTextStreamWatcher =
                        new TextStreamWatcher(executor.getStderr(), ConcurrencyUtils.getAsyncTaskService(),
                            new CapturingTextOutReceiver("") {

                                @Override
                                public synchronized void addOutput(String line) {
                                    super.addOutput(line);
                                    validator.validatePythonVersion(getBufferedOutput().toString());
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
                            } catch (IOException e) {
                                logger.error(PYTHON_VALIDATION_ERROR, e);
                            } catch (InterruptedException e) {
                                logger.error(PYTHON_VALIDATION_ERROR, e);
                            }
                        }
                    });

                    try {
                        task.get(PYTHON_TEST_TIMEOUT, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        executor.cancel();
                    } catch (ExecutionException e) {
                        logger.error(PYTHON_VALIDATION_ERROR, e);
                    } finally {
                        executor.cancel();
                    }

                    processPythonValidationResult(validator, messages, pythonInstallation);

                } catch (IOException | InterruptedException e) {
                    logger.error(PYTHON_VALIDATION_ERROR, e);
                }

            }

        }

        return messages;
    }

    private void createPythonExecutionSuccessfulMessage(PythonVersionRegexValidator validator) {
        LogFactory.getLog(this.getClass())
            .debug("Python Version Used: " + validator.getMajorPythonVersion() + "."
                + validator.getMinorPythonVersion() + "."
                + validator.getMicroPythonVersion());
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
            createPythonExecutionSuccessfulMessage(validator);
        }
    }
    
    /**
     * 
     * @return the {@link PythonVersionRegexValidator}. Intended for unit testing.
     */
    public PythonVersionRegexValidator createPythonVersionRegexValidator() {
        return new PythonVersionRegexValidator();
    }

    /**
     * 
     * @author David Scholz
     *
     */
    public class PythonVersionRegexValidator {

        private static final int MINUS_ONE = -1;

        private boolean pythonExecutionSuccessful = false;

        private final Pattern matchPattern;

        private int majorPythonVersion = MINUS_ONE;

        private int minorPythonVersion = MINUS_ONE;

        private int microPythonVersion = MINUS_ONE;

        PythonVersionRegexValidator() {
            this.matchPattern = Pattern.compile("^Python\\s([0-9]+)\\.([0-9]+)\\.([0-9]+)");
        }

        /**
         * 
         * @param bufferedOutput stdout or stderr of the python exe.
         */
        public synchronized void validatePythonVersion(String bufferedOutput) {
            Matcher matcher = matchPattern.matcher(bufferedOutput);
            if (matcher.find()) {
                majorPythonVersion = Integer.parseInt(matcher.group(1));
                minorPythonVersion = Integer.parseInt(matcher.group(2));
                microPythonVersion = Integer.parseInt(matcher.group(3));
            }

            if (majorPythonVersion == 2 && minorPythonVersion >= 6) {
                pythonExecutionSuccessful = true;
            } else if (majorPythonVersion == 3) {
                pythonExecutionSuccessful = true;
            }
        }

        public boolean isPythonExecutionSuccessful() {
            return pythonExecutionSuccessful;
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

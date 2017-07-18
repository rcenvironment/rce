/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.components.script.execution.validator.ScriptComponentValidator.ExecutionOutputCache;
import de.rcenvironment.components.script.execution.validator.ScriptComponentValidator.PythonVersionRegexValidator;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreater;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.scripting.python.PythonComponentConstants;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * 
 * Test for the validator of the Script Component.
 *
 * @author Jascha Riedel
 * @author David Scholz
 * @author Doreen Seider (caching test)
 */
public class ScriptComponentValidatorTest {

    private static final String PYTHON = "Python";

    private ComponentDescription componentDescription;

    private ScriptComponentValidator validator;

    private ComponentDescriptionMockCreater componentDescriptionHelper;

    private PythonVersionRegexValidator regexValidator;

    /**
     * 
     * Sets up the validator and the componentDescriptionHelper.
     *
     */
    @Before
    public void setUp() {
        validator = new ScriptComponentValidator();
        regexValidator = validator.createPythonVersionRegexValidator();
        componentDescriptionHelper = new ComponentDescriptionMockCreater();
    }

    /**
     * 
     * Tests if validator identifies an empty script as error.
     *
     */
    @Test
    public void testEmptyScript() {

        componentDescriptionHelper.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, "");

        componentDescription = componentDescriptionHelper.createComponentDescriptionMock();

        List<ComponentValidationMessage> messages = validator.validate(componentDescription, false);

        assertEquals(messages.size(), 1);
        assertEquals(messages.get(0).getType(), ComponentValidationMessage.Type.ERROR);
    }

    /**
     *
     * Tests if validator identifies the default script as warning.
     *
     */
    @Test
    public void testDefaultScript() {
        componentDescriptionHelper.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT,
            ScriptComponentConstants.DEFAULT_SCRIPT_WITHOUT_COMMENTS_AND_IMPORTS);

        componentDescription = componentDescriptionHelper.createComponentDescriptionMock();

        List<ComponentValidationMessage> messages = validator.validate(componentDescription, false);

        assertEquals(messages.size(), 1);
        assertEquals(messages.get(0).getType(), ComponentValidationMessage.Type.WARNING);
    }

    /**
     * 
     * Tests if validator identifies a wrong python execution path as error.
     *
     */
    @Test
    public void testWrongPythonInstallationPath() {

        componentDescriptionHelper.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, "glajerkleg");
        componentDescriptionHelper.addConfigurationValue(ScriptComponentConstants.SCRIPT_LANGUAGE, PYTHON);
        componentDescriptionHelper.addConfigurationValue(PythonComponentConstants.PYTHON_INSTALLATION, "dksljgil");

        componentDescription = componentDescriptionHelper.createComponentDescriptionMock();

        List<ComponentValidationMessage> messages = validator.validate(componentDescription, true);

        assertEquals(messages.size(), 1);
        assertEquals(messages.get(0).getType(), ComponentValidationMessage.Type.ERROR);

    }

    /**
     * Tests if validator works correctly with different python versions which are provided with an absolut path defined in a properties
     * file.
     * 
     * @throws IOException e
     * @throws FileNotFoundException s
     */
    @Test
    public void testDifferentPythonVersionsManually() throws FileNotFoundException, IOException {
        componentDescriptionHelper.addConfigurationValue(ScriptComponentConstants.SCRIPT_LANGUAGE, PYTHON);
        componentDescriptionHelper.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, "Hellö");

        File f = new File("resources/");
        File[] matchingFiles = f.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("properties");
            }
        });

        if (matchingFiles.length == 0) {
            return;
        } else {
            for (File configFile : matchingFiles) {
                Properties prop = new Properties();
                prop.load(new FileInputStream(configFile.getAbsolutePath()));

                for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                    componentDescriptionHelper.addConfigurationValue(PythonComponentConstants.PYTHON_INSTALLATION,
                        entry.getValue().toString());

                    componentDescription = componentDescriptionHelper.createComponentDescriptionMock();

                    List<ComponentValidationMessage> messages = validator.validate(componentDescription, true);
                    assertEquals(messages.size(), 0);
                }
            }
        }
    }

    /**
     * 
     * Tests if the regex which checks the version of the used python installation works correctly.
     * 
     */
    @Test
    public void testPythonVersionRegexForPlainPython() {

        String python279 = "Python 2.7.9";
        String python2711 = "Python 2.7.11";
        final int eleven = 11;
        String python2x = "Python 2";
        String python352 = "Python 3.5.2";
        String python35 = "Python 3.5";
        String python3 = "Python 3";

        regexValidator.validatePythonVersion(python279);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 2 && regexValidator.getMinorPythonVersion() > 6
            && regexValidator.getMicroPythonVersion() == 9);

        regexValidator.validatePythonVersion(python2711);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 2 && regexValidator.getMinorPythonVersion() > 6
            && regexValidator.getMicroPythonVersion() == eleven);

        regexValidator.validatePythonVersion(python2x);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 2);

        regexValidator.validatePythonVersion(python352);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 3 && regexValidator.getMinorPythonVersion() == 5
            && regexValidator.getMicroPythonVersion() == 2);

        regexValidator.validatePythonVersion(python35);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 3 && regexValidator.getMinorPythonVersion() == 5);

        regexValidator.validatePythonVersion(python3);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 3);
    }

    /**
     * 
     * Tests if the regex which checks the version of the used python installation works correctly if anaconda is used (background:
     * stdout/stderr for "--version" differs from plain python).
     * 
     */
    @Test
    public void testPythonVersionRegexForAnaconda() {
        String validAnacondaOutput = "Python 3.5.2 :: Anaconda 4.2.0 (64-bit)";
        String validOutput = "Python 3 Anaconda";

        regexValidator.validatePythonVersion(validAnacondaOutput);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 3 && regexValidator.getMinorPythonVersion() == 5
            && regexValidator.getMicroPythonVersion() == 2);

        regexValidator.validatePythonVersion(validOutput);
        assertTrue(regexValidator.isPythonExecutionSuccessful());
        assertTrue(regexValidator.getMajorPythonVersion() == 3);
    }

    /**
     * 
     * Tests if regex handles wrong outputs correctly.
     * 
     */
    @Test
    public void testPythonVersionRegexWithFalseOutput() {
        String falseOutput = "Hellö Wörld! UHD is nice. UHD is your friend.";
        String python1 = "Python 1";

        regexValidator.validatePythonVersion(falseOutput);
        assertFalse(regexValidator.isPythonExecutionSuccessful());

        regexValidator.validatePythonVersion(python1);
        assertFalse(regexValidator.isPythonExecutionSuccessful());
    }

    /**
     * Tests if the cache is used as intended. The test doesn't cover the actual caching functionality encapsulated in
     * {@link ExecutionOutputCache}.
     * 
     * @throws IOException on unexpected error
     * @throws InterruptedException on unexpected error
     * 
     */
    @Test
    public void testCacheUsage() throws IOException, InterruptedException {

        CacheTestingScriptComponentValidator testableValidator = new CacheTestingScriptComponentValidator();

        componentDescriptionHelper.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, "print('hello python')");
        componentDescriptionHelper.addConfigurationValue(ScriptComponentConstants.SCRIPT_LANGUAGE, PYTHON);
        componentDescriptionHelper.addConfigurationValue(PythonComponentConstants.PYTHON_INSTALLATION, testableValidator.somePath);
        ComponentDescription compDesc = componentDescriptionHelper.createComponentDescriptionMock();

        List<ComponentValidationMessage> validationMessages1 = testableValidator.validate(compDesc, true);
        assertEquals(1, validationMessages1.size());
        List<ComponentValidationMessage> validationMessages2 = testableValidator.validate(compDesc, true);
        assertEquals(validationMessages1.size(), validationMessages2.size());
        assertEquals(validationMessages1.get(0).getAbsoluteMessage(), validationMessages2.get(0).getAbsoluteMessage());
        
        EasyMock.verify(testableValidator.commandLineExecutorMock);
        EasyMock.verify(testableValidator.executionOutputCacheMock);
    }

    /**
     * {@link ScriptComponentValidator} that allows to inject the {@link LocalApacheCommandLineExecutor}.
     * 
     * @author Doreen Seider
     */
    private static final class CacheTestingScriptComponentValidator extends ScriptComponentValidator {

        private final String somePath = "/some/path";

        private final String someOut = "some out";

        private LocalApacheCommandLineExecutor commandLineExecutorMock;

        private ExecutionOutputCache executionOutputCacheMock;

        @Override
        protected LocalApacheCommandLineExecutor createCommandLineExecutor(File workDirPath) throws IOException {

            commandLineExecutorMock = EasyMock.createStrictMock(LocalApacheCommandLineExecutor.class);
            commandLineExecutorMock.start(StringUtils.format("\"%s\" --version", somePath, " --version"));
            EasyMock.expectLastCall().once();
            InputStream outInStreamStub = IOUtils.toInputStream(someOut);
            InputStream errInStreamStub = IOUtils.toInputStream("");
            EasyMock.expect(commandLineExecutorMock.getStdout()).andReturn(outInStreamStub);
            EasyMock.expect(commandLineExecutorMock.getStderr()).andReturn(errInStreamStub);
            try {
                EasyMock.expect(commandLineExecutorMock.waitForTermination()).andReturn(0);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            EasyMock.expect(commandLineExecutorMock.cancel()).andReturn(true);
            EasyMock.replay(commandLineExecutorMock);

            return commandLineExecutorMock;
        }

        @Override
        protected ExecutionOutputCache createExecutionOutputCache() {

            executionOutputCacheMock = EasyMock.createStrictMock(ExecutionOutputCache.class);
            EasyMock.expect(executionOutputCacheMock.containsKey(somePath)).andReturn(false);
            List<String> output = new ArrayList<>();
            output.add(someOut + "\n"); // see CpaturingTextOutputReceiver for '\n'
            executionOutputCacheMock.put(EasyMock.eq(somePath), EasyMock.eq(output));
            EasyMock.expect(executionOutputCacheMock.containsKey(somePath)).andReturn(true);
            EasyMock.expect(executionOutputCacheMock.get(somePath)).andReturn(output);
            EasyMock.replay(executionOutputCacheMock);

            return executionOutputCacheMock;
        }
    }
}

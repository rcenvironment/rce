/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.components.script.execution.validator.ScriptComponentValidator.PythonValidationResult;
import de.rcenvironment.components.script.execution.validator.ScriptComponentValidator.PythonVersionRegexValidator;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreator;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.scripting.python.PythonComponentConstants;

/**
 * 
 * Test for the validator of the Script Component.
 *
 * @author Jascha Riedel
 * @author David Scholz
 * @author Doreen Seider (caching test)
 * @author Thorsten Sommer (fixed Python validation tests, added new tests)
 */
public class ScriptComponentValidatorTest {

    private static final String PYTHON = "Python";

    private static final String PYTHON_DUMMY_PATH = "/dummy";

    private ComponentDescription componentDescription;

    private ScriptComponentValidator validator;

    private ComponentDescriptionMockCreator componentDescriptionHelper;

    /**
     * 
     * Sets up the validator and the componentDescriptionHelper.
     *
     */
    @Before
    public void setUp() {
        validator = new ScriptComponentValidator();
        componentDescriptionHelper = new ComponentDescriptionMockCreator();
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

        final String python279 = "Python 2.7.9";
        final String python2711 = "Python 2.7.11";
        final int eleven = 11;
        final String python2x = "Python 2";
        final String python352 = "Python 3.5.2";
        final String python35 = "Python 3.5";
        final String python3 = "Python 3";

        PythonVersionRegexValidator regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(python279, PYTHON_DUMMY_PATH);
        PythonValidationResult result = regexValidator.getValidationResult();
        assertTrue(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 2 && result.getMinorPythonVersion() > 6
            && result.getMicroPythonVersion() == 9);

        regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(python2711, PYTHON_DUMMY_PATH);
        result = regexValidator.getValidationResult();
        assertTrue(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 2 && result.getMinorPythonVersion() > 6
            && result.getMicroPythonVersion() == eleven);

        regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(python2x, PYTHON_DUMMY_PATH);
        result = regexValidator.getValidationResult();
        assertFalse(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 2);

        regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(python352, PYTHON_DUMMY_PATH);
        result = regexValidator.getValidationResult();
        assertTrue(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 3 && result.getMinorPythonVersion() == 5
            && result.getMicroPythonVersion() == 2);

        regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(python35, PYTHON_DUMMY_PATH);
        result = regexValidator.getValidationResult();
        assertTrue(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 3 && result.getMinorPythonVersion() == 5);

        regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(python3, PYTHON_DUMMY_PATH);
        result = regexValidator.getValidationResult();
        assertTrue(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 3);
    }

    /**
     * 
     * Tests if the regex which checks the version of the used python installation works correctly if anaconda is used (background:
     * stdout/stderr for "--version" differs from plain python).
     * 
     */
    @Test
    public void testPythonVersionRegexForAnaconda() {
        final String validAnacondaOutput = "Python 3.5.2 :: Anaconda 4.2.0 (64-bit)";
        final String validOutput = "Python 3 Anaconda";

        PythonVersionRegexValidator regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(validAnacondaOutput, PYTHON_DUMMY_PATH);
        PythonValidationResult result = regexValidator.getValidationResult();
        assertTrue(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 3 && result.getMinorPythonVersion() == 5
            && result.getMicroPythonVersion() == 2);

        regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(validOutput, PYTHON_DUMMY_PATH);
        result = regexValidator.getValidationResult();
        assertTrue(result.isPythonExecutionSuccessful());
        assertTrue(result.getMajorPythonVersion() == 3);
    }

    /**
     * 
     * Tests if regex handles wrong outputs correctly.
     * 
     */
    @Test
    public void testPythonVersionRegexWithFalseOutput() {
        final String falseOutput = "Hellö Wörld! UHD is nice. UHD is your friend.";
        final String python1 = "Python 1";

        PythonVersionRegexValidator regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(falseOutput, PYTHON_DUMMY_PATH);
        PythonValidationResult result = regexValidator.getValidationResult();
        assertFalse(result.isPythonExecutionSuccessful());

        regexValidator = new PythonVersionRegexValidator();
        regexValidator.validatePythonVersion(python1, PYTHON_DUMMY_PATH);
        result = regexValidator.getValidationResult();
        assertFalse(result.isPythonExecutionSuccessful());
    }

    /**
     * 
     * Tests if validation works if version string occurs later.
     * 
     */
    @Test
    public void testPythonVersionNotInFirstLine() {
        final String[] multiLineVersion = {
            "Hellö Wörld! UHD is nice. UHD is your friend.",
            "Another useless line",
            "Python 3.5.2",
            "a useless line afterwards"
        };
        
        final PythonVersionRegexValidator anotherValidator = new PythonVersionRegexValidator();
        for (String line : multiLineVersion) {
            anotherValidator.validatePythonVersion(line, PYTHON_DUMMY_PATH);
        }

        PythonValidationResult result = anotherValidator.getValidationResult();
        assertTrue("Python version not in the first line was not detected", result.isPythonExecutionSuccessful());
    }

    /**
     * This test case ensures that the class {@link PythonValidationResult} works as intended.
     */
    @Test
    public void testPythonValidationResult() {

        // Simulate Python 3.4.5:
        final int expectedMajor = 3;
        final int expectedMinor = 4;
        final int expectedMicro = 5;
        final String path = "/bin/python";
        final boolean state = true;
        
        PythonValidationResult result = new PythonValidationResult(path, expectedMajor, expectedMinor, expectedMicro, state);
        assertEquals("The major version is wrong", expectedMajor, result.getMajorPythonVersion());
        assertEquals("The minor version is wrong", expectedMinor, result.getMinorPythonVersion());
        assertEquals("The mico version is wrong", expectedMicro, result.getMicroPythonVersion());
        assertEquals("The path is wrong", path, result.getPythonPath());
        assertTrue("The state was wrong", result.isPythonExecutionSuccessful());
    }

    /**
     * This test case ensures that the class {@link PythonValidationResult} handled null as intended.
     */
    @Test(expected = NullPointerException.class)
    public void testPythonValidationResultNull() {

        // Simulate Python 3.4.5:
        final int expectedMajor = 3;
        final int expectedMinor = 4;
        final int expectedMicro = 5;
        final String path = null;

        new PythonValidationResult(path, expectedMajor, expectedMinor, expectedMicro, false);
    }

    /**
     * This test case ensures that the class {@link PythonValidationResult} handled empty strings as intended.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testPythonValidationResultEmptyStrings() {

        // Simulates Python 3.4.5:
        final int expectedMajor = 3;
        final int expectedMinor = 4;
        final int expectedMicro = 5;
        final String path = "";

        new PythonValidationResult(path, expectedMajor, expectedMinor, expectedMicro, false);
    }

    /**
     * Ensures that a fake placeholder gets not recognized as actual placeholder.
     */
    @Test
    public void testPythonValidationResultPlaceholder1() {
        final PythonValidationResult fakePlaceholder = new PythonValidationResult("/none", -1, -1, -1, false);
        assertFalse("A fake placeholder was detected as correct placeholder.", fakePlaceholder.isPlaceholder());
    }

    /**
     * Ensures that the actual placeholder gets recognized.
     */
    @Test
    public void testPythonValidationResultPlaceholder2() {
        final PythonValidationResult correctPlaceholder = PythonValidationResult.DEFAULT_NONE_PLACEHOLDER;
        assertTrue("The correct placeholder was not recognized as such.", correctPlaceholder.isPlaceholder());
    }

    /**
     * Ensures that the placeholder is never successfully.
     */
    @Test
    public void testPythonValidationResultPlaceholder3() {
        final PythonValidationResult correctPlaceholder = PythonValidationResult.DEFAULT_NONE_PLACEHOLDER;
        assertFalse("The placeholder cannot have the state 'successfully'.", correctPlaceholder.isPythonExecutionSuccessful());
    }
}

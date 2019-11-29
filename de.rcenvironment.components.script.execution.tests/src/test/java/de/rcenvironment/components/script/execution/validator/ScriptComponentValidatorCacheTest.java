/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.script.execution.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.rcenvironment.components.script.execution.validator.ScriptComponentValidator.PythonValidationResult;

/**
 * 
 * This class contains test cases for the validator cache. Due to the nature of singleton constructs, the test cases are numbered and sorted
 * by JUnit to guarantee the execution order.
 *
 * @author Thorsten Sommer
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScriptComponentValidatorCacheTest {

    private static final String KNOWN_PATH_1 = "/is/known/1";

    private static final String KNOWN_PATH_2 = "/is/known/2";

    private static final String KNOWN_PATH_3 = "/is/known/3";

    private static final String UNKNOWN_PATH = "/a/unknown/path";

    private static final String PLACEHOLDER_PATH = "/none";

    private static final String WRONG_MAJOR_VERSION = "The major version is wrong";

    private static final String WRONG_MINOR_VERSION = "The minor version is wrong";

    private static final String WRONG_MICRO_VERSION = "The micro version is wrong";

    private static final String WRONG_PATH = "The path is wrong";

    private static final String WRONG_STATE = "The state is wrong";

    private static final String KNOWN_PATH_UNKNOWN = "A known path was unknown";

    /**
     * 
     * Ensures that the singleton instance of the cache is never null.
     *
     */
    @Test
    public void test01SingeltonNoneNull() {
        assertNotNull("The validator cache is null", ScriptComponentValidatorCache.getCache());
    }

    /**
     * 
     * Ensures that an unknown path is not known.
     *
     */
    @Test
    public void test02UnknownPath1() {
        // Unknown path:
        assertFalse("A unknown path was known", ScriptComponentValidatorCache.getCache().isPathValidated(UNKNOWN_PATH));
    }

    /**
     * 
     * Ensures that the previously unknown path is still unknown.
     *
     */
    @Test
    public void test03UnknownPath2() {
        // Still unknown:
        assertFalse("A still unknown path was known", ScriptComponentValidatorCache.getCache().isPathValidated(UNKNOWN_PATH));
    }

    /**
     * 
     * Ensures that null is not a valid path. There should be no exception thrown.
     *
     */
    @Test
    public void test04NullPath() {
        assertFalse("A null string was a known path", ScriptComponentValidatorCache.getCache().isPathValidated(null));
    }

    /**
     * 
     * Ensures that an empty string is not a valid path. There should be no exception thrown.
     *
     */
    @Test
    public void test05EmptyPath() {
        assertFalse("An empty string was a known path", ScriptComponentValidatorCache.getCache().isPathValidated(""));
    }

    /**
     * 
     * Adds a valid result to the cache.
     *
     */
    @Test
    public void test06AddValidResult() {
        final PythonValidationResult result = new PythonValidationResult(KNOWN_PATH_1, 1, 2, 3, true);
        ScriptComponentValidatorCache.getCache().addOrUpdateValidationResult(result);
    }

    /**
     * 
     * Ensures that a NPE gets thrown if someone tries to add null to the cache.
     *
     */
    @Test(expected = NullPointerException.class)
    public void test07AddNullResult() {
        ScriptComponentValidatorCache.getCache().addOrUpdateValidationResult(null);
    }

    /**
     * 
     * Ensures that adding a valid result leads to a cache state where the path is known.
     *
     */
    @Test
    public void test08KnownPath1() {
        PythonValidationResult result = new PythonValidationResult(KNOWN_PATH_2, 4, 5, 6, true);
        ScriptComponentValidatorCache.getCache().addOrUpdateValidationResult(result);

        assertTrue(KNOWN_PATH_UNKNOWN, ScriptComponentValidatorCache.getCache().isPathValidated(KNOWN_PATH_2));
    }

    /**
     * 
     * Ensures that both used paths are still known.
     *
     */
    @Test
    public void test09KnownPath2() {
        assertTrue(KNOWN_PATH_UNKNOWN, ScriptComponentValidatorCache.getCache().isPathValidated(KNOWN_PATH_1));
        assertTrue(KNOWN_PATH_UNKNOWN, ScriptComponentValidatorCache.getCache().isPathValidated(KNOWN_PATH_2));
    }

    /**
     * 
     * Ensures that the correct result gets yielded from the cache.
     *
     */
    @Test
    public void test10KnownResult1() {
        
        PythonValidationResult result = ScriptComponentValidatorCache.getCache().getValidationResult(KNOWN_PATH_1);
        
        assertEquals(WRONG_MAJOR_VERSION, 1, result.getMajorPythonVersion());
        assertEquals(WRONG_MINOR_VERSION, 2, result.getMinorPythonVersion());
        assertEquals(WRONG_MICRO_VERSION, 3, result.getMicroPythonVersion());
        assertEquals(WRONG_PATH, "/is/known/1", result.getPythonPath());
        assertTrue(WRONG_STATE, result.isPythonExecutionSuccessful());
    }

    /**
     * 
     * Ensures that the correct result gets yielded from the cache.
     *
     */
    @Test
    public void test11KnownResult2() {

        PythonValidationResult result = ScriptComponentValidatorCache.getCache().getValidationResult(KNOWN_PATH_2);

        assertEquals(WRONG_MAJOR_VERSION, 4, result.getMajorPythonVersion());
        assertEquals(WRONG_MINOR_VERSION, 5, result.getMinorPythonVersion());
        assertEquals(WRONG_MICRO_VERSION, 6, result.getMicroPythonVersion());
        assertEquals(WRONG_PATH, "/is/known/2", result.getPythonPath());
        assertTrue(WRONG_STATE, result.isPythonExecutionSuccessful());
    }

    /**
     * 
     * Ensures that a cached result can be updated.
     *
     */
    @Test
    public void test12UpdateOfResult() {

        final String path = KNOWN_PATH_3;
        final int expectedMajor = 6;
        final int expectedMinor = 7;
        final int expectedMicro = 8;

        // Write state 1 (validation failed):
        PythonValidationResult result1 = new PythonValidationResult(path, expectedMajor, expectedMinor, expectedMicro, false);
        ScriptComponentValidatorCache.getCache().addOrUpdateValidationResult(result1);

        // Ensure its state:
        PythonValidationResult read = ScriptComponentValidatorCache.getCache().getValidationResult(path);
        assertEquals(WRONG_MAJOR_VERSION, expectedMajor, read.getMajorPythonVersion());
        assertEquals(WRONG_MINOR_VERSION, expectedMinor, read.getMinorPythonVersion());
        assertEquals(WRONG_MICRO_VERSION, expectedMicro, read.getMicroPythonVersion());
        assertEquals(WRONG_PATH, path, read.getPythonPath());
        assertFalse(WRONG_STATE, read.isPythonExecutionSuccessful());

        // Write state 2 (validation passed):
        PythonValidationResult result2 = new PythonValidationResult(path, expectedMajor, expectedMinor, expectedMicro, true);
        ScriptComponentValidatorCache.getCache().addOrUpdateValidationResult(result2);

        // Ensure its state:
        read = ScriptComponentValidatorCache.getCache().getValidationResult(path);
        assertEquals(WRONG_MAJOR_VERSION, expectedMajor, read.getMajorPythonVersion());
        assertEquals(WRONG_MINOR_VERSION, expectedMinor, read.getMinorPythonVersion());
        assertEquals(WRONG_MICRO_VERSION, expectedMicro, read.getMicroPythonVersion());
        assertEquals(WRONG_PATH, path, read.getPythonPath());
        assertTrue(WRONG_STATE, read.isPythonExecutionSuccessful());
    }

    /**
     * 
     * Ensures that it is not possible to add the placeholder value of the result class (i.e.
     * {@link PythonValidationResult.DEFAULT_NONE_PLACEHOLDER}) to the cache. There should be no exception thrown.
     *
     */
    @Test
    public void test13AddingPlaceholder() {

        // Try to add the placeholder to the cache:
        final PythonValidationResult placeholder = PythonValidationResult.DEFAULT_NONE_PLACEHOLDER;
        ScriptComponentValidatorCache.getCache().addOrUpdateValidationResult(placeholder);

        // Nothing should happens. No exception in intended. Further, the placeholder must not added to the cache!
        assertFalse("The placeholder must not be added to the cache",
            ScriptComponentValidatorCache.getCache().isPathValidated(PLACEHOLDER_PATH));
    }
}

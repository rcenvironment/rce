/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration.bootstrap;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link LaunchParameters}.
 *
 * @author Tobias Brieden
 */
public class LaunchParametersTest {

    

    private static final String TEST2 = "test2";
    private static final String TEST1 = "test1";
    private static final String LONG_PROFILE_FLAG = "--profile";
    private static final String SHORT_PROFILE_FLAG = "-p";
    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    /**
     * Tests if the correct parameter is retrieved.
     * 
     * @throws ParameterException unexpected
     */
    @Test
    public void testSingleOccurence() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG, TEST1);
        
        assertEquals(TEST1, LaunchParameters.getInstance().getNamedParameter(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
    }
    
    /**
     * Tests if the correct parameter is retrieved.
     * 
     * @throws ParameterException unexpected
     */
    @Test
    public void testValueAfterParamterIsReturned() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG, TEST1, TEST2);
        
        assertEquals(TEST1, LaunchParameters.getInstance().getNamedParameter(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
    }
    
    /**
     * Tests if no parameter is retrieved.
     * 
     * @throws ParameterException unexpected
     */
    @Test
    public void testMissingParameterAfterKey() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG);
        
        assertNull(LaunchParameters.getInstance().getNamedParameter(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
        assertTrue(LaunchParameters.getInstance().containsToken(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
    }
    
    /**
     * Tests exception on conflicting options.
     * 
     * @throws ParameterException expected
     */
    @Test
    public void testMultipleConflictingOptions() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG, TEST1, SHORT_PROFILE_FLAG, TEST2);
        
        expectedException.expect(ParameterException.class);
        LaunchParameters.getInstance().getNamedParameter(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG);
    }

    /**
     * Tests exception on conflicting options.
     * 
     * @throws ParameterException expected
     */
    @Test
    public void testConflictingShortAndLongOptions() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG, TEST1, LONG_PROFILE_FLAG, TEST2);
        
        expectedException.expect(ParameterException.class);
        LaunchParameters.getInstance().getNamedParameter(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG);
    }
    
    /**
     * Tests if no parameter is retrieved.
     * 
     * @throws ParameterException unexpected
     */
    @Test
    public void testKeyNotPresentInOptions() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG, TEST1, SHORT_PROFILE_FLAG, TEST2);
        
        assertNull(LaunchParameters.getInstance().getNamedParameter("-t", "--trofile"));
        assertFalse(LaunchParameters.getInstance().containsToken("-t", "--trofile"));
    }
    
    /**
     * Tests if known parameters and there values are removed from the list of tokens.
     * 
     * @throws ParameterException unexpected
     */
    @Test
    public void testIfKnownTokenToIgnoreIsRemoved() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG, "-vm", "thisshouldberemoved");
        
        assertNull(LaunchParameters.getInstance().getNamedParameter(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
        assertTrue(LaunchParameters.getInstance().containsToken(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
    }
    
    /**
     * Tests if unkonwn options/parameters are not mistaken as values for previous parameters.
     * 
     * @throws ParameterException unexpected
     */
    @Test
    public void testIfOptionAfterProfileOptionIsRecognized() throws ParameterException {
        LaunchParameterTestUtils.setParameters(SHORT_PROFILE_FLAG, "-cm");
        
        assertNull(LaunchParameters.getInstance().getNamedParameter(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
        assertTrue(LaunchParameters.getInstance().containsToken(SHORT_PROFILE_FLAG, LONG_PROFILE_FLAG));
    }
}

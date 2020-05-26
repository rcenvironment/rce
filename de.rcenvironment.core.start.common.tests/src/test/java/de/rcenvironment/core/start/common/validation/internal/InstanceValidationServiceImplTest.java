/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.internal;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationService;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Tests for {@linkplain InstanceValidationServiceImpl} .
 *
 * @author Tobias Rodehutskors
 */
public class InstanceValidationServiceImplTest {

    private static final String B_PASSED = "B passed";

    private static final String A_PASSED = "A passed";

    private InstanceValidationService service;

    /** Before. */
    @Before
    public void setUp() {
        service = new InstanceValidationServiceImpl();
    }

    /**
     * Dummy class to make sure the class names differ.
     */
    public abstract class ValidatorA implements InstanceValidator {
    }

    /**
     * Dummy class to make sure the class names differ.
     */
    public abstract class ValidatorB implements InstanceValidator {
    }

    private void validateInstanceAndCheckResults(int numPassed, int numFailedProceed, int numFailedShutdown) {
        Map<InstanceValidationResultType, List<InstanceValidationResult>> results = service.validateInstance();
        assertEquals(numPassed, results.get(InstanceValidationResultType.PASSED).size());
        assertEquals(numFailedProceed, results.get(InstanceValidationResultType.FAILED_CONFIRMATION_REQUIRED).size());
        assertEquals(numFailedShutdown, results.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED).size());
    }

    /**
     * Validator without a required predecessor. Should be executed without error.
     */
    @Test
    public void testValidatorWithoutPredecessors() {
        InstanceValidator validatorA = EasyMock.createStrictMock(ValidatorA.class);
        EasyMock.expect(validatorA.getNecessaryPredecessors()).andReturn(null);
        EasyMock.expect(validatorA.validate()).andReturn(InstanceValidationResultFactory.createResultForPassed(A_PASSED));
        EasyMock.replay(validatorA);

        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorA);

        validateInstanceAndCheckResults(1, 0, 0);

        EasyMock.verify(validatorA);
    }

    /**
     * Two validators. One requiring the other in already correct order. Should be executed without error.
     */
    @Test
    public void testValidatorWithExistingPredecessor() {

        InstanceValidator validatorA = EasyMock.createStrictMock(ValidatorA.class);
        EasyMock.expect(validatorA.getNecessaryPredecessors()).andReturn(null);
        EasyMock.expect(validatorA.validate()).andReturn(InstanceValidationResultFactory.createResultForPassed(A_PASSED));
        EasyMock.replay(validatorA);

        InstanceValidator validatorB = EasyMock.createStrictMock(ValidatorB.class);
        List<Class<? extends InstanceValidator>> validatorBnecessaryPredecessors = new LinkedList<Class<? extends InstanceValidator>>();
        validatorBnecessaryPredecessors.add(validatorA.getClass());
        EasyMock.expect(validatorB.getNecessaryPredecessors()).andReturn(validatorBnecessaryPredecessors);
        EasyMock.expect(validatorB.validate()).andReturn(InstanceValidationResultFactory.createResultForPassed(B_PASSED));
        EasyMock.replay(validatorB);

        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorA);
        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorB);

        validateInstanceAndCheckResults(2, 0, 0);

        EasyMock.verify(validatorA);
        EasyMock.verify(validatorB);
    }

    /**
     * Two validators. One requiring the other which requires reordering. Should be executed without error.
     */
    @Test
    public void testValidatorWithExistingPredecessorReorderingRequired() {

        InstanceValidator validatorA = EasyMock.createStrictMock(ValidatorA.class);
        EasyMock.expect(validatorA.getNecessaryPredecessors()).andReturn(null);
        EasyMock.expect(validatorA.validate()).andReturn(InstanceValidationResultFactory.createResultForPassed(A_PASSED));
        EasyMock.replay(validatorA);

        InstanceValidator validatorB = EasyMock.createStrictMock(ValidatorB.class);
        List<Class<? extends InstanceValidator>> validatorBnecessaryPredecessors = new LinkedList<Class<? extends InstanceValidator>>();
        validatorBnecessaryPredecessors.add(validatorA.getClass());
        EasyMock.expect(validatorB.getNecessaryPredecessors()).andReturn(validatorBnecessaryPredecessors).atLeastOnce();
        EasyMock.expect(validatorB.validate()).andReturn(InstanceValidationResultFactory.createResultForPassed(B_PASSED));
        EasyMock.replay(validatorB);

        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorB);
        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorA);

        validateInstanceAndCheckResults(2, 0, 0);

        EasyMock.verify(validatorA);
        EasyMock.verify(validatorB);
    }

    /**
     * Two validators. One requiring the other which is not available to the {@link InstanceValidationServiceImpl}. Should result in an
     * error.
     */
    @Test
    public void testValidatorWithMissingPredecessor() {

        InstanceValidator validatorA = EasyMock.createStrictMock(ValidatorA.class);
        EasyMock.replay(validatorA);

        InstanceValidator validatorB = EasyMock.createStrictMock(ValidatorB.class);
        List<Class<? extends InstanceValidator>> validatorBnecessaryPredecessors = new LinkedList<Class<? extends InstanceValidator>>();
        validatorBnecessaryPredecessors.add(validatorA.getClass());
        EasyMock.expect(validatorB.getNecessaryPredecessors()).andReturn(validatorBnecessaryPredecessors).atLeastOnce();
        EasyMock.replay(validatorB);

        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorB);

        validateInstanceAndCheckResults(0, 0, 1);

        EasyMock.verify(validatorA);
        EasyMock.verify(validatorB);
    }

    /**
     * Two validator with a cyclic dependency. Should result in an error.
     */
    @Test
    public void testValidatorsWithCyclicDependency() {

        InstanceValidator validatorA = EasyMock.createStrictMock(ValidatorA.class);
        InstanceValidator validatorB = EasyMock.createStrictMock(ValidatorB.class);

        List<Class<? extends InstanceValidator>> validatorAnecessaryPredecessors = new LinkedList<Class<? extends InstanceValidator>>();
        validatorAnecessaryPredecessors.add(validatorB.getClass());
        EasyMock.expect(validatorA.getNecessaryPredecessors()).andReturn(validatorAnecessaryPredecessors).atLeastOnce();
        EasyMock.replay(validatorA);

        List<Class<? extends InstanceValidator>> validatorBnecessaryPredecessors = new LinkedList<Class<? extends InstanceValidator>>();
        validatorBnecessaryPredecessors.add(validatorA.getClass());
        EasyMock.expect(validatorB.getNecessaryPredecessors()).andReturn(validatorBnecessaryPredecessors).atLeastOnce();
        EasyMock.replay(validatorB);

        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorA);
        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorB);

        validateInstanceAndCheckResults(0, 0, 1);

        EasyMock.verify(validatorA);
        EasyMock.verify(validatorB);
    }

    /**
     * Two validators. One requiring the other in already correct order. But the first validator fails. Should not execute the second
     * validator.
     */
    @Test
    public void testValidatorWithFailedPredecessor() {

        InstanceValidator validatorA = EasyMock.createStrictMock(ValidatorA.class);
        EasyMock.expect(validatorA.getNecessaryPredecessors()).andReturn(null);
        EasyMock.expect(validatorA.validate()).andReturn(
            InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown("", "", ""));
        EasyMock.replay(validatorA);

        InstanceValidator validatorB = EasyMock.createStrictMock(ValidatorB.class);
        List<Class<? extends InstanceValidator>> validatorBnecessaryPredecessors = new LinkedList<Class<? extends InstanceValidator>>();
        validatorBnecessaryPredecessors.add(validatorA.getClass());
        EasyMock.expect(validatorB.getNecessaryPredecessors()).andReturn(validatorBnecessaryPredecessors).atLeastOnce();
        EasyMock.replay(validatorB);

        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorA);
        ((InstanceValidationServiceImpl) service).bindInstanceValidator(validatorB);

        validateInstanceAndCheckResults(0, 0, 2);

        EasyMock.verify(validatorA);
        EasyMock.verify(validatorB);
    }
}

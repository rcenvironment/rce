/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationService;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link InstanceValidationService}.
 * 
 * @author Christian Weiss
 * @author Doreen Seider
 * @author Tobias Rodehutskors
 * @author Alexander Weinert (Validation failure with user confirmation)
 */
public class InstanceValidationServiceImpl implements InstanceValidationService {

    protected Map<Class<? extends InstanceValidator>, InstanceValidationResult> executedValidators = new HashMap<>();

    private final Log log = LogFactory.getLog(InstanceValidationServiceImpl.class);

    private List<InstanceValidator> validators = new LinkedList<>();

    protected void bindInstanceValidator(final InstanceValidator newValidation) {
        validators.add(newValidation);
    }

    /**
     * Checks for a given InstanceValidator if all required predecessors of this InstanceValidator have been executed and passed.
     * 
     * @param validator The InstanceValidator whose predecessors should be checked.
     * @return True, if all required predecessors have already been executed; false otherwise.
     */
    protected boolean necessaryValidatorsExecuted(InstanceValidator validator) {

        List<Class<? extends InstanceValidator>> necessaryPredecessors = validator.getNecessaryPredecessors();

        if (necessaryPredecessors == null) {
            return true;
        }

        for (Class<? extends InstanceValidator> necessaryPredecessor : necessaryPredecessors) {
            if (!executedValidators.containsKey(necessaryPredecessor)) {
                return false;
            }

            InstanceValidationResult instanceValidationResult = executedValidators.get(necessaryPredecessor);
            if (!InstanceValidationResultType.PASSED.equals(instanceValidationResult.getType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates the RCE instance. All InstanceValidators need to be present before the validation starts.
     * 
     * @return map with {@link InstanceValidationResult}s. For each validator one result exists. It is guaranteed that all of the possible
     *         {@link InstanceValidationResultType}s are provided as keys of the map, i.e., each value of
     *         {@link InstanceValidationResultType} is mapped at least to an empty list.
     */
    @Override
    public Map<InstanceValidationResultType, List<InstanceValidationResult>> validateInstance() {

        final Map<InstanceValidationResultType, List<InstanceValidationResult>> results = new EnumMap<>(InstanceValidationResultType.class);
        results.put(InstanceValidationResultType.PASSED, new ArrayList<InstanceValidationResult>());
        results.put(InstanceValidationResultType.FAILED_CONFIRMATION_REQUIRED, new ArrayList<InstanceValidationResult>());
        results.put(InstanceValidationResultType.FAILED_RECOVERY_REQUIRED, new ArrayList<InstanceValidationResult>());
        results.put(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED, new ArrayList<InstanceValidationResult>());

        // list of all validators which need to be executed in order to validate the instance
        final List<InstanceValidator> pendingValidators = new LinkedList<>(validators);

        int pendingValidatorsBeforeIteration;
        do {
            pendingValidatorsBeforeIteration = pendingValidators.size();

            // helper list, since we cannot safely remove items from a list while iterating over it
            final List<InstanceValidator> currentValidators = new LinkedList<>(pendingValidators);
            for (final InstanceValidator validator : currentValidators) {

                // skip a validator if its required predecessors have not been executed yet
                if (!necessaryValidatorsExecuted(validator)) {
                    continue;
                }

                InstanceValidationResult result;
                try {
                    result = validator.validate();
                } catch (RuntimeException e) {
                    log.error(StringUtils.format("Unexpected exception from instance validator '%s'", validator.getClass().getName()), e);
                    result = InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                        "Instance validator", "An unexpected exception occurred during instance validation. See log for more details.");
                }
                results.get(result.getType()).add(result);

                executedValidators.put(validator.getClass(), result);
                pendingValidators.remove(validator);
            }
            // if the list of pending validators does not decrease we have an unresolveable dependency
        } while (!pendingValidators.isEmpty() && pendingValidators.size() < pendingValidatorsBeforeIteration);

        // shutdown the instance if not all validators have been executed
        if (!pendingValidators.isEmpty()) {
            final boolean allPendingDueToConfirmation = pendingValidators.stream()
                .map(this::validatorPendingDueToConfirmationRequired)
                .reduce(true, (acc, current) -> acc && current).booleanValue();

            if (!allPendingDueToConfirmation) {
                log.error(StringUtils.format("%d instance validator have not been executed, as there have missing predecessors: ",
                    pendingValidators.size()));
                for (InstanceValidator validator : pendingValidators) {
                    log.error(StringUtils.format("%s has not been executed.", validator.getClass().getName()));
                }

                InstanceValidationResult result = InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                    "Instance validator", "Not all necessary validators have been executed. See log for more details.");
                results.get(result.getType()).add(result);
            }
        }

        return results;
    }

    private boolean validatorPendingDueToConfirmationRequired(InstanceValidator validator) {
        final List<InstanceValidationResult> predecessorResults = validator.getNecessaryPredecessors().stream()
            .map(predecessorClass -> executedValidators.get(predecessorClass))
            .collect(Collectors.toList());

        // It would be possible to fold the following checks into a map/reduce statement. For the sake of readability, however, we write
        // them down in long form.
        for (InstanceValidationResult validationResult : predecessorResults) {
            
            // Some validation result being null implies that the corresponding validator has not run. It would be more readable to extract
            // this check to a local variable called, e.g., `validatorHasRun`, but this confuses the java compiler, which complains that
            // validationResult may be null inside the if-clause. Hence, we check for validationResult != null inside the if-condition
            // itself.
            if (validationResult == null) {
                return false;
            }
            
            final InstanceValidationResultType validationResultType = validationResult.getType();
            final boolean isResultPassed = InstanceValidationResultType.PASSED.equals(validationResultType);
            final boolean isResultConfirmationRequired =
                InstanceValidationResultType.FAILED_RECOVERY_REQUIRED.equals(validationResultType);
            if (!(isResultPassed || isResultConfirmationRequired)) {
                return false;
            }
        }
        return true;
    }

}

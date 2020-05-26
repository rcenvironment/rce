/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.start.common.validation.spi;

import java.util.List;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;

/**
 * Performs checks against the RCE instance upon startup.
 *
 * @author Christian Weiss
 * @author Tobias Rodehutskors
 */
public interface InstanceValidator {
    
    /**
     * Perform checking tasks.
     * 
     * @return instance of {@link InstanceValidationResult}
     */
    InstanceValidationResult validate();

    /**
     * 
     * @return A list of InstanceValidators which need to be executed prior to the execution of this InstanceValidator.
     */
    List<Class<? extends InstanceValidator>> getNecessaryPredecessors();
}

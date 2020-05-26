/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.api;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Validates the RCE instance thru the registered {@link InstanceValidator}s.
 * 
 * @author Christian Weiss
 * @author Doreen Seider
 */
public interface InstanceValidationService {

    /**
     * Validates the RCE instance.
     * 
     * @return Map with {@link InstanceValidationResult}s. For each validator one result exists. It is guaranteed that all of the possible
     *         {@link InstanceValidationResultType}s are provided as keys of the map, i.e., each value of
     *         {@link InstanceValidationResultType} is mapped at least to an empty list.
     */
    Map<InstanceValidationResultType, List<InstanceValidationResult>> validateInstance();
}

/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
     * @return {@link InstanceValidationResult}s sorted by {@link InstanceValidationResultType}
     */
    Map<InstanceValidationResultType, List<InstanceValidationResult>> validateInstance();
}

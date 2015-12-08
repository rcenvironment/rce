/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 */
public class InstanceValidationServiceImpl implements InstanceValidationService {

    private final Log log = LogFactory.getLog(InstanceValidationServiceImpl.class);
    
    private List<InstanceValidator> validators = new LinkedList<InstanceValidator>();

    protected void bindInstanceValidator(final InstanceValidator newValidation) {
        validators.add(newValidation);  
    }

    /**
     * Validates the RCE instance.
     * 
     * @return map with {@link InstanceValidationResult}s. For each validator one result exists. It is guaranteed that all of the possible
     *         {@link InstanceValidationResultType}s are provided as keys of the map.
     */
    @Override
    public Map<InstanceValidationResultType, List<InstanceValidationResult>> validateInstance() {
        
        final Map<InstanceValidationResultType, List<InstanceValidationResult>> results = new HashMap<>();
        results.put(InstanceValidationResultType.PASSED, new ArrayList<InstanceValidationResult>());
        results.put(InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED, new ArrayList<InstanceValidationResult>());
        results.put(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED, new ArrayList<InstanceValidationResult>());
        
        Iterator<InstanceValidator> validatorIterator = validators.iterator();
        
        // use iterator as the validator map might change during iterating, as binding can happen all of the time
        while (validatorIterator.hasNext()) {
            InstanceValidator validator = validatorIterator.next();
            InstanceValidationResult result;
            try {
                result = validator.validate();
            } catch (RuntimeException e) {
                log.error(StringUtils.format("Unexpected exception from instance validator '%s'", validator.getClass().getName()), e);
                result = InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                    "Instance validator", "An unexpected excetion occurred during instance validation. See log for more details.");
            }
            results.get(result.getType()).add(result);
        }
        return results;
    }

}

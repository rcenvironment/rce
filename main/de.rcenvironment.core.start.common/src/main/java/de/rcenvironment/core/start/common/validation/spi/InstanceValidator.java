/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.start.common.validation.spi;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;

/**
 * Performs checks against the RCE instance upon startup.
 *
 * @author Christian Weiss
 */
public interface InstanceValidator {
    
    /**
     * Perform checking tasks.
     * 
     * @return instance of {@link InstanceValidationResult}
     */
    InstanceValidationResult validate();

}

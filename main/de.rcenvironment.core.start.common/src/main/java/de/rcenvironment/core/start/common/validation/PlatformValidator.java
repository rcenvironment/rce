/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.start.common.validation;

import java.util.Collection;

/**
 * A validator to perform checks against the RCE platform upon startup.
 *
 * @author Christian Weiss
 */
// TODO >5.0.0: rename ("StartupValidator", maybe?) - misc_ro
public interface PlatformValidator {
    
    /**
     * Perform checking tasks.
     * 
     * @return a collection of {@link PlatformMessage}s informing about occurrences
     */
    Collection<PlatformMessage> validatePlatform();

}

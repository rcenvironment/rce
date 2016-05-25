/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.spi;

import java.util.List;

/**
 * Abstract implementation of InstanceValidator.
 *
 * @author Tobias Rodehutskors
 */
public abstract class DefaultInstanceValidator implements InstanceValidator {

    @Override
    public List<Class<? extends InstanceValidator>> getNecessaryPredecessors() {
        return null;
    }
}

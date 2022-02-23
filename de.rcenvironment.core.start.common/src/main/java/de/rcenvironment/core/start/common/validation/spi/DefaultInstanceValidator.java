/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

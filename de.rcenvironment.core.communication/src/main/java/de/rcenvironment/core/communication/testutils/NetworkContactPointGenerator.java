/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.model.NetworkContactPoint;

/**
 * Abstract generator for {@link NetworkContactPoint}s. Not named "...Factory" as implementations
 * may have an internal state (for example, acquired network resources), which might be unexpected
 * for a factory.
 * 
 * @author Robert Mischke
 */
public interface NetworkContactPointGenerator {

    /**
     * Generates a new {@link NetworkContactPoint}. The new instance is expected to be different
     * (with regard to network behavior and "equals" identity) from all previously-generated contact
     * points.
     * 
     * @return the new contact point
     */
    NetworkContactPoint createContactPoint();
}

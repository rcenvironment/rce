/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.service;

import java.util.Collection;

/**
 * An interface for OSGi-DS services that allows them to register additional services without sub-classing their interfaces (see Mantis
 * #9423).
 * 
 * @author Robert Mischke
 */
public interface AdditionalServicesProvider {

    /**
     * @return the {@link AdditionalServiceDeclaration}s that define the services that the provider wants to register
     */
    // TODO naming: "define" vs. "declare"?
    Collection<AdditionalServiceDeclaration> defineAdditionalServices();
}

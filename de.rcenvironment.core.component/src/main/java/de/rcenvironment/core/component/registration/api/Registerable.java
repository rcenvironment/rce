/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.registration.api;

import de.rcenvironment.core.component.execution.api.Component;

/**
 * Tag interface to register the components as an OSGi service at the OSGi registry in order to get the properties of the component (stored
 * in OSGI-INF/[component].xml). The {@link Component} interface should not be used for that in order to avoid not allowed method invocation
 * when registered temporary.
 * 
 * @author Doreen Seider
 */
public interface Registerable {
}

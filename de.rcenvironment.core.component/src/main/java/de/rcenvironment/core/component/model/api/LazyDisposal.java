/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks components which {@link Component#dispose()} method should be called on workflow disposal and
 * not immediately after the component had reached a final state (finished, failed, cancelled), which is the default behavior.
 * 
 * @author Doreen Seider
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LazyDisposal {

}

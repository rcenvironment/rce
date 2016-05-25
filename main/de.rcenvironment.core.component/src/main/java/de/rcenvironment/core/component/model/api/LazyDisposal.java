/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks components which {@link Component#dispose()} method should be called on workflow disposal and
 * not immediately after the component had reached a final state (Finished, failed, cancelled), which is the default behavior.
 * 
 * @author Doreen Seider
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LazyDisposal {

}

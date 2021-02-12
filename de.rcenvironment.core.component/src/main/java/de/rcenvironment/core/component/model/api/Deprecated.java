/*
 * Copyright 2006-2021 DLR, Germany
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
 * Marks components as deprecated.
 * 
 * @author Doreen Seider
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Deprecated {

}

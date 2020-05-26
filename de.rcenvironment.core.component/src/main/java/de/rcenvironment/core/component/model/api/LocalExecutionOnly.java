/*
 * Copyright 2006-2020 DLR, Germany
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
 * Marks components which are only locally executable. Reasons can be: platform-specific
 * configuration values like file system path.
 * 
 * @author Doreen Seider
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LocalExecutionOnly {

}

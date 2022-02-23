/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.security;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A marker for methods that are allowed to be called via RPC.
 * 
 * Note that this annotation must be placed on the <b>implementation</b> of the target method;
 * applying it to an <b>interface</b> method will have no effect.
 * 
 * @author Robert Mischke
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AllowRemoteAccess {

}

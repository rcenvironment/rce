/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.rpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark service interfaces that are allowed to be used by remote instances. All methods in these interfaces are intended for
 * remote invocation. When it makes sense for a service implementation to provide both local and remote methods via service interface, it
 * should implement both a local and a remote interface, with only the remote interface carrying this annotation.
 * <p>
 * To ensure explicit exception handling, every method in a {@link RemotableService} must declare {@link RemoteOperationException} as a
 * checked exception. This requirement will be enforced by the RPC implementation once migration is complete.
 * 
 * @author Robert Mischke
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemotableService {

}

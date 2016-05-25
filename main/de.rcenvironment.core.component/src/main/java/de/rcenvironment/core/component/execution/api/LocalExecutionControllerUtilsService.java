/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import org.osgi.framework.BundleContext;

/**
 * Utility methods used during component/workflow execution.
 * 
 * @author Doreen Seider
 * 
 */
public interface LocalExecutionControllerUtilsService {

    /**
     * Retrieves {@link ExecutionController}s as OSGi services from the OSGi registry on the base of its execution identifier.
     * 
     * Note: Should be moved somewhere else (more generic bundle) in RCE.
     * 
     * @param <T> subclass of {@link ExecutionController}
     * @param controllerInterface sub interface of {@link ExecutionController}
     * @param executionId execution identifier to consider
     * @param bundleContext {@link BundleContext} of the calling bundle
     * @return instance of an {@link ExecutionController} of the type T with controllerInterface
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    <T extends ExecutionController> T getExecutionController(Class<T> controllerInterface, String executionId,
        BundleContext bundleContext) throws ExecutionControllerException;

}

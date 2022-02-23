/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.Map;

import org.osgi.framework.BundleContext;

/**
 * Utility methods used during component/workflow execution.
 * 
 * @author Doreen Seider
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
     * @return instance of an {@link ExecutionController} of the type T
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    <T extends ExecutionController> T getExecutionController(Class<T> controllerInterface, String executionId,
        BundleContext bundleContext) throws ExecutionControllerException;
    
    /**
     * Retrieves all {@link ExecutionController}s as OSGi services from the OSGi registry.
     * 
     * @param <T> subclass of {@link ExecutionController}
     * @param controllerInterface sub interface of {@link ExecutionController}
     * @param bundleContext {@link BundleContext} of the calling bundle
     * @return instances of {@link ExecutionController} of the type T mapped to its execution identifier
     */
    <T extends ExecutionController>  Map<String, T> getExecutionControllers(Class<T> controllerInterface, BundleContext bundleContext);

}

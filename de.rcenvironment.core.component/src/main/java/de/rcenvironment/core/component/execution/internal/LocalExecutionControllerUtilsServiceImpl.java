/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.component.execution.api.ExecutionConstants;
import de.rcenvironment.core.component.execution.api.ExecutionController;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link LocalExecutionControllerUtilsService}.
 * 
 * @author Doreen Seider
 */
public class LocalExecutionControllerUtilsServiceImpl implements LocalExecutionControllerUtilsService {

    @Override
    public <T extends ExecutionController> T getExecutionController(Class<T> controllerInterface, String executionId,
        BundleContext bundleContext) throws ExecutionControllerException {

        String filter = createPropertyFilter(executionId);
        try {
            ServiceReference<?>[] serviceReferences = bundleContext.getServiceReferences(controllerInterface.getName(),
                filter);
            if (serviceReferences != null) {
                for (ServiceReference<?> ref : serviceReferences) {
                    ExecutionController exeCtrl = (ExecutionController) bundleContext.getService(ref);
                    if (exeCtrl != null) {
                        return (T) exeCtrl;                        
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // should not happen
            LogFactory.getLog(LocalExecutionControllerUtilsServiceImpl.class).error(StringUtils.format("Filter '%s' is not valid", filter));
        } catch (IllegalStateException e) {
            // TODO change this once "graceful shutdown" is implemented; then it is an error at any time
            LogFactory.getLog(LocalExecutionControllerUtilsServiceImpl.class).warn(
                "The software bundle providing this workflow component is unavailable; "
                    + "if this happens at any other time than shutdown, it is an error", e);
        }
        throw new ExecutionControllerException(StringUtils.format("Component or workflow (%s) "
            + "(more precisely its execution controller) does not exist (anymore)", executionId));
    }
    
    private static String createPropertyFilter(String executionId) {
        return StringUtils.format("(%s=%s)", ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, executionId);
    }

    @Override
    public <T extends ExecutionController> Map<String, T> getExecutionControllers(Class<T> controllerInterface,
        BundleContext bundleContext) {
        Map<String, T> exeControllers = new HashMap<>();

        String filter = null;
        try {
            ServiceReference<?>[] serviceReferences = bundleContext.getServiceReferences(controllerInterface.getName(), filter);
            if (serviceReferences != null) {
                for (ServiceReference<?> serviceRef : serviceReferences) {
                    exeControllers.put((String) serviceRef.getProperty(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY),
                        (T) bundleContext.getService(serviceRef));
                }
            }
        } catch (InvalidSyntaxException e) {
            // should not happen as filter is null
            LogFactory.getLog(LocalExecutionControllerUtilsServiceImpl.class).error(StringUtils.format("Unexpected error"), e);
        }
        return exeControllers;
    }
}

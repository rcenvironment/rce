/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

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
            LogFactory.getLog(LocalExecutionControllerUtilsServiceImpl.class).warn("Bundle seems to be shutted down. "
                + "If this occurs at any other time than shut down, it is an error", e);
        }
        throw new ExecutionControllerException(StringUtils.format("Component or workflow (%s) "
            + "(more precisely its execution controller) does not exist (anymore)", executionId));
    }
    
    private static String createPropertyFilter(String executionId) {
        return StringUtils.format("(%s=%s)", ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, executionId);
    }
}

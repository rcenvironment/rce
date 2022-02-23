/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.execution;

import java.io.File;

import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelService;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.notification.DistributedNotificationService;


/**
 * Excel component parent (abstract) class for handling RCE specific things.
 *
 * @author Markus Kunde
 */
@LazyDisposal
public abstract class ExcelRCEComponent extends DefaultComponent {

    private static final int NOTIFICATION_BUFFER = 1000;
    
    protected DistributedNotificationService notificationService;

    protected ExcelService excelService;

    protected TypedDatumFactory typedDatumFactory;
    
    protected TypedDatumConverter typedDatumConverter;
    
    protected ComponentDataManagementService dataManagementService;
    
    protected ComponentContext componentContext;
    
    protected ComponentLog componentLog;
    
    protected abstract void executingOneStep() throws ComponentException;
    
    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        this.componentLog = componentContext.getLog();
    }
    
    @Override
    public boolean treatStartAsComponentRun() {
        boolean isDriver = Boolean.valueOf(componentContext.getConfigurationValue(ExcelComponentConstants.DRIVER));
        return isDriver || componentContext.getInputs().isEmpty();
    }
    
    @Override
    public void start() throws ComponentException {
        notificationService = componentContext.getService(DistributedNotificationService.class);
        excelService = componentContext.getService(ExcelService.class);
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        typedDatumConverter = componentContext.getService(TypedDatumService.class).getConverter();
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        
        File originExcelFile =
            ExcelUtils.getAbsoluteFile(componentContext.getConfigurationValue(ExcelComponentConstants.XL_FILENAME));
        if (originExcelFile == null) {
            throw new ComponentException("Cannot prepare Excel component. Maybe filename/path is wrong?");
        }

        notificationService.setBufferSize(componentContext.getExecutionIdentifier() + ExcelComponentConstants.NOTIFICATION_SUFFIX,
            NOTIFICATION_BUFFER);
        
        if (treatStartAsComponentRun()) {
            executingOneStep();
        }
    }
    
    @Override
    public void processInputs() throws ComponentException {
        executingOneStep();       
    }
    
    @Override
    public void dispose() {
        if (componentContext != null && notificationService != null) {
            // clears notification buffer
            notificationService.removePublisher(componentContext.getExecutionIdentifier() 
                + ExcelComponentConstants.NOTIFICATION_SUFFIX);            
        }
    }
}

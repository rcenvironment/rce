/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;
import de.rcenvironment.cpacs.utils.common.components.CpacsChannelFilter;

/**
 * Tool wrapper.
 * 
 * @author Markus Kunde
 */
public class ToolWrapper extends DefaultComponent {

    protected static DistributedNotificationService notificationService;
    
    protected static ComponentDataManagementService dataManagementService;

    /** path to bundle resources. */
    private static final String RESOURCE_PREFIX = "/resources/";

    private ComponentContext componentContext;
    
    private TypedDatumFactory typedDatumFactory;
    
    private CpacsToolWrapper cpacsToolWrapper;

    @Override
    public void dispose() {
        if (cpacsToolWrapper != null) {
            cpacsToolWrapper.cleanUp();
        }
    }
    
    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }
    
    @Override
    public void start() throws ComponentException {
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        notificationService = componentContext.getService(DistributedNotificationService.class);
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        
        // Create tool instance
        final String componentIndentifier = componentContext.getComponentIdentifier().split(ComponentConstants.ID_SEPARATOR)[0];
        final String wrapperFilename = RESOURCE_PREFIX
            + componentIndentifier.substring(CpacsComponentConstants.COMPONENT_ID.length() + 1)
            + CpacsComponentConstants.CPACSWRAPPER_FILEEXTENTION;
        final InputStream is = getClass().getResourceAsStream(wrapperFilename);
        try {
            cpacsToolWrapper = new CpacsToolWrapper(this, is);
        } catch (IOException e) {
            throw new ComponentException(e);
        }
        try {
            if (!cpacsToolWrapper.prepare()) {
                throw new ComponentException("Could not prepare Cpacs tool.");
            }
        } catch (IOException e) {
            throw new ComponentException(e);
        }
    }
    
    @Override
    public void processInputs() throws ComponentException {
        String cpacs = CpacsChannelFilter.getCpacs(ChameleonCommonConstants.CHAMELEON_CPACS_NAME, componentContext);
        String directory = CpacsChannelFilter.getDirectory(componentContext);
        Map<String, TypedDatum> variableInputs = CpacsChannelFilter.getVariableInputs(componentContext);

        try {
            cpacs = cpacsToolWrapper.toolRun(cpacs, directory, variableInputs);
            directory = cpacsToolWrapper.getDirectoryUUID();
        } catch (IOException e) {
            throw new ComponentException(e);
        } catch (DataTypeException e) {
            throw new ComponentException(e);
        }

        for (String outputName : componentContext.getOutputs()) {
            if (outputName.equals(ChameleonCommonConstants.CHAMELEON_CPACS_NAME)) {
                componentContext.writeOutput(outputName, typedDatumFactory.createFileReference(cpacs,
                    ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME));
            } else if (outputName.equals(ChameleonCommonConstants.DIRECTORY_CHANNELNAME) && directory != null) {
                componentContext.writeOutput(outputName, typedDatumFactory.createFileReference(directory,
                    CpacsComponentConstants.RETURN_ZIP_NAME));
            }
        }
    }
    
    protected ComponentContext getComponentContext() {
        return componentContext;
    }
}

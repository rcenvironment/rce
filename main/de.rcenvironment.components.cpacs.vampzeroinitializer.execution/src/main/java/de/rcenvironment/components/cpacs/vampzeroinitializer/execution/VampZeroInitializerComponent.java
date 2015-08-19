/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.execution;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;
import de.rcenvironment.cpacs.utils.common.components.CpacsChannelFilter;
import de.rcenvironment.cpacs.utils.common.components.XmlComponentHistoryDataItem;
import de.rcenvironment.cpacs.utils.common.xml.ComponentVariableMapper;

/**
 * The "source" function based on aicraft predesign parameters.
 * 
 * @author Arne Bachmann
 * @author Markus Litz
 * @author Markus Kunde
 */
public class VampZeroInitializerComponent extends DefaultComponent {

    private static final Log LOG = LogFactory.getLog(VampZeroInitializerComponent.class);
    
    private ComponentContext componentContext;
    
    private ComponentDataManagementService dataManagementService;

    /** XML Content from the configuration map (created by the view). */
    private String xmlContent;

    private XmlComponentHistoryDataItem historyDataItem = null;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }
    
    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getInputs().isEmpty();
    }
    
    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        
        final String vampZeroInputs = componentContext.getConfigurationValue(VampZeroInitializerComponentConstants.XMLCONTENT);

        if ((vampZeroInputs != null) && !vampZeroInputs.isEmpty()) { // allow non-initialized run of component (if in workflow
                                                                     // but not connected)
            xmlContent = vampZeroInputs;
        } else {
            LOG.warn("No output defined in component.");
        }
        
        if (treatStartAsComponentRun()) {
            processInputs();
        }
    }
    
    @Override
    public void processInputs() throws ComponentException {
        if (xmlContent == null || xmlContent.isEmpty()) {
            return;
        }
        initializeNewHistoryDataItem();
        File tempFile = null;
        final ComponentVariableMapper varMapper = new ComponentVariableMapper();
        String cpacs = "";
        try {
            tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("VAMPZeroInitializer-*.xml");
            FileUtils.writeStringToFile(tempFile, xmlContent);
            cpacs = dataManagementService.createTaggedReferenceFromLocalFile(componentContext, tempFile,
                    ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME);
        } catch (IOException e) {
            LOG.error(e);
        }
        if (tempFile != null) {
            // Dynamic input values
            Map<String, TypedDatum> variableInputs = CpacsChannelFilter.getVariableInputs(componentContext);
            if (historyDataItem != null && !variableInputs.isEmpty()) {
                historyDataItem.setPlainXMLFileReference(cpacs);
            }
            try {
                varMapper.updateXMLWithInputs(tempFile.getAbsolutePath(), variableInputs, componentContext);
            } catch (DataTypeException e1) {
                throw new ComponentException(e1.getMessage(), e1);
            }
            // Cpacs with dynamic input values in history object
            final FileReferenceTD fileReference;
            try {
                fileReference = dataManagementService.createFileReferenceTDFromLocalFile(componentContext, tempFile,
                    ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME);
                // Write Output
                componentContext.writeOutput(ChameleonCommonConstants.CHAMELEON_CPACS_NAME, fileReference);
            } catch (IOException e) {
                LOG.debug(e);
            }
            try {
                varMapper.updateOutputsFromXML(tempFile.getAbsolutePath(), ChameleonCommonConstants.CHAMELEON_CPACS_NAME, componentContext);
            } catch (DataTypeException e1) {
                throw new ComponentException(e1.getMessage(), e1);
            }
            storeHistoryDataItem();
        }
    }

    @Override
    public void tearDown(FinalComponentState state) {
        switch (state) {
        case FAILED:
            storeHistoryDataItem();
            break;
        default:
            break;
        }
        super.tearDown(state);
    }
    
    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new XmlComponentHistoryDataItem(VampZeroInitializerComponentConstants.COMPONENT_ID);
        }
    }

    private void storeHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

}

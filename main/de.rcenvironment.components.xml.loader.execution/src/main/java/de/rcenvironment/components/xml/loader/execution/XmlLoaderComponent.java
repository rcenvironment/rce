/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.loader.execution;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.xml.loader.common.XmlLoaderComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;
import de.rcenvironment.cpacs.utils.common.components.XmlComponentHistoryDataItem;
import de.rcenvironment.cpacs.utils.common.xml.ComponentVariableMapper;

/**
 * Implementing class for Source functionality with file support.
 * 
 * @author Markus Kunde
 * @author Markus Litz
 * @author Jan Flink
 */
public class XmlLoaderComponent extends DefaultComponent {

    private ComponentContext componentContext;
    
    private ComponentDataManagementService dataManagementService;

    /** XML Content from configuration map. */
    private String xmlContent = null;

    private XmlComponentHistoryDataItem historyDataItem = null;
    
    private File tempFile = null;

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
        
        xmlContent = componentContext.getConfigurationValue(XmlLoaderComponentConstants.XMLCONTENT);

        if (xmlContent == null) {
            String errrorMessage = "No XML file configured";
            componentContext.printConsoleLine(errrorMessage, ConsoleRow.Type.STDERR);
            throw new ComponentException(errrorMessage);
        }
        
        if (treatStartAsComponentRun()) {
            processInputs();
        }
    }
    
    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();
        
        final FileReferenceTD fileReference;
        final ComponentVariableMapper varMapper = new ComponentVariableMapper();

        try {
            tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("XMLLoader-*.xml");
            FileUtils.writeStringToFile(tempFile, xmlContent);

            // Dynamic input values to XML
            Map<String, TypedDatum> variableInputs = new HashMap<>();
            for (String inputName : componentContext.getInputsWithDatum()) {
                if (componentContext.isDynamicInput(inputName)) {
                    variableInputs.put(inputName, componentContext.readInput(inputName));
                }
            }
            if (historyDataItem != null && !variableInputs.isEmpty()) {
                historyDataItem.setPlainXMLFileReference(dataManagementService.createFileReferenceTDFromLocalFile(componentContext,
                    tempFile, ChameleonCommonConstants.XML_PLAIN_FILENAME).getFileReference());
            }
            varMapper.updateXMLWithInputs(tempFile.getAbsolutePath(), variableInputs, componentContext);

            // Output dynamic values to XML
            varMapper.updateOutputsFromXML(tempFile.getAbsolutePath(), XmlLoaderComponentConstants.ENDPOINT_NAME_XML, componentContext);
            fileReference =
                dataManagementService.createFileReferenceTDFromLocalFile(componentContext, tempFile,
                    componentContext.getInstanceName() + ChameleonCommonConstants.XML_APPENDIX_FILENAME);
        } catch (IOException e) {
            throw new ComponentException("Writing XML file to temporary file failed", e);
        } catch (DataTypeException e) {
            throw new ComponentException("Adding input values to XML file failed", e);
        }

        componentContext.writeOutput(XmlLoaderComponentConstants.ENDPOINT_NAME_XML, fileReference);
        storeHistoryDataItem();
        deleteTempFile();
    }
    
    @Override
    public void tearDown(FinalComponentState state) {
        if (state == FinalComponentState.FAILED) {
            storeHistoryDataItem();
            deleteTempFile();
        }
    }
    
    
    private void deleteTempFile() {
        if (tempFile != null) {
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFile);
            } catch (IOException e) {
                LogFactory.getLog(getClass()).warn("Failed to delete temp file: " + tempFile.getAbsolutePath(), e);
            }
        }
    }
    
    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new XmlComponentHistoryDataItem(XmlLoaderComponentConstants.COMPONENT_ID);
        }
    }

    private void storeHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

}

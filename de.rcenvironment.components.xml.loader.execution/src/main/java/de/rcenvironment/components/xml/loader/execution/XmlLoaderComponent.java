/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.xml.XMLComponentConstants;
import de.rcenvironment.core.component.xml.XmlComponentHistoryDataItem;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Implementing class for Source functionality with file support.
 * 
 * @author Markus Kunde
 * @author Markus Litz
 * @author Jan Flink
 * @author Brigitte Boden
 */
public class XmlLoaderComponent extends DefaultComponent {

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    /** XML Content from configuration map. */
    private String xmlContent = null;

    private XmlComponentHistoryDataItem historyDataItem = null;

    private File tempFile = null;

    private EndpointXMLService endpointXmlUtils;

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
        endpointXmlUtils = componentContext.getService(EndpointXMLService.class);

        xmlContent = componentContext.getConfigurationValue(XmlLoaderComponentConstants.XMLCONTENT);

        if (xmlContent == null) {
            throw new ComponentException("No XML content configured that should be loaded and sent");
        }

        if (treatStartAsComponentRun()) {
            processInputs();
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();

        try {
            tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("XMLLoader-*.xml");
            FileUtils.writeStringToFile(tempFile, xmlContent);
        } catch (IOException e) {
            throw new ComponentException("Failed to write XML file into a temporary file "
                + "(that is required for XML Loader)", e);
        }

        // Dynamic input values to XML
        Map<String, TypedDatum> variableInputs = new HashMap<>();
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)) {
                variableInputs.put(inputName, componentContext.readInput(inputName));
            }
        }

        if (historyDataItem != null && !variableInputs.isEmpty()) {
            try {
                historyDataItem.setPlainXMLFileReference(dataManagementService.createFileReferenceTDFromLocalFile(componentContext,
                    tempFile, XMLComponentConstants.XML_PLAIN_FILENAME).getFileReference());
                storeHistoryDataItem();
            } catch (IOException e) {
                String errorMessage = "Failed to store plain XML file into the data management"
                    + "; it will not be available in the workflow data browser";
                String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(
                    LogFactory.getLog(XmlLoaderComponent.class), errorMessage, e);
                componentContext.getLog().componentError(errorMessage, e, errorId);
            }
        }

        try {
            endpointXmlUtils.updateXMLWithInputs(tempFile, variableInputs, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to add dynamic input values to the XML file", e);
        }

        final FileReferenceTD fileReference;
        try {
            fileReference = dataManagementService.createFileReferenceTDFromLocalFile(componentContext, tempFile,
                componentContext.getInstanceName() + XMLComponentConstants.XML_APPENDIX_FILENAME);
        } catch (IOException e) {
            throw new ComponentException("Failed to store XML file into the data management - "
                + "if it is not stored in the data management, it can not be sent as output value", e);
        }
        componentContext.writeOutput(XmlLoaderComponentConstants.OUTPUT_NAME_XML, fileReference);

        try {
            // Output dynamic values from XML
            endpointXmlUtils.updateOutputsFromXML(tempFile, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to extract dynamic output values from the XML file", e);
        }

        deleteTempFile();
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        storeHistoryDataItem();
        deleteTempFile();
    }

    private void deleteTempFile() {
        if (tempFile != null) {
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFile);
            } catch (IOException e) {
                LogFactory.getLog(getClass()).error("Failed to delete temp file: " + tempFile.getAbsolutePath(), e);
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

/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.xml.values.execution;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.xml.values.common.XmlValuesComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Implementation of TextLinesReceiver for XMLValues.
 *
 * @author Adrian Stock
 * @author Jan Flink
 */

public class XmlValuesComponent extends DefaultComponent {

    private static final String FAILED_TO_DELETE_TEMP_FILE = "Failed to delete temp file: ";

    private static final Log LOG = LogFactory.getLog(XmlValuesComponent.class);

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private EndpointXMLService endpointXmlUtils;

    private File tempMainFile = null;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public final void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        endpointXmlUtils = componentContext.getService(EndpointXMLService.class);
    }

    @Override
    public void processInputs() throws ComponentException {
        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        FileReferenceTD mainXml = (FileReferenceTD) componentContext.readInput(XmlValuesComponentConstants.ENDPOINT_NAME_XML);
        Map<String, TypedDatum> variableInputs = new HashMap<>();
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)
                && !inputName.equals(XmlValuesComponentConstants.ENDPOINT_NAME_XML)) {
                variableInputs.put(inputName, componentContext.readInput(inputName));
            }
        }

        try {
            tempMainFile = tempFileService.createTempFileFromPattern("XMLValues-*.xml");

            dataManagementService.copyReferenceToLocalFile(mainXml.getFileReference(), tempMainFile,
                componentContext.getStorageNetworkDestination());
        } catch (IOException e) {
            throw new ComponentException(
                "Failed to write XML file into a temporary file " + "(that is required for XML Values)",
                e);
        }

        try {
            endpointXmlUtils.updateXMLWithInputs(tempMainFile, variableInputs, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to add dynamic input values to the XML file", e);
        }

        final FileReferenceTD fileReference;
        try {
            fileReference = dataManagementService.createFileReferenceTDFromLocalFile(componentContext,
                tempMainFile, mainXml.getFileName());
        } catch (IOException e) {
            throw new ComponentException(
                "Failed to store updated XML file into the data management - "
                    + "if it is not stored in the data management, it can not be sent as output value",
                e);
        }
        
        try {
            endpointXmlUtils.updateOutputsFromXML(tempMainFile, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException(
                    "Failed to extract the entry at " + endpointXmlUtils.getRecentXpath() + " from the XML file", e);
        }

        componentContext.writeOutput(XmlValuesComponentConstants.ENDPOINT_NAME_XML, fileReference);
        deleteTempFiles();
    }

    private void deleteTempFiles() {
        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        try {
            if (tempMainFile != null) {
                tempFileService.disposeManagedTempDirOrFile(tempMainFile);
            }
        } catch (IOException e) {
            LOG.error(FAILED_TO_DELETE_TEMP_FILE + tempMainFile.getAbsolutePath(), e);
        }
    }
}

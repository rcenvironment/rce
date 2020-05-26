/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.execution;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
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
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.core.utils.common.xml.api.XMLMapperService;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;
import de.rcenvironment.toolkit.utils.text.AbstractTextLinesReceiver;

/**
 * Implementing class for XMLMerger functionality with file support.
 * 
 * @author Markus Kunde
 * @author Miriam Lenk#
 * @author Jan FLink
 * @author Brigitte Boden
 */
public class XmlMergerComponent extends DefaultComponent {

    /**
     * Implementation of TextLinesReceiver for XMLMerger.
     *
     * @author Brigitte Boden
     */
    private final class XMLMergerTextLinesReceiver extends AbstractTextLinesReceiver {

        @Override
        public void addLine(String line) {
            componentContext.getLog().componentInfo(line);
        }
    }

    private static final String FAILED_TO_DELETE_TEMP_FILE = "Failed to delete temp file: ";

    private static final Log LOG = LogFactory.getLog(XmlMergerComponent.class);

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private EndpointXMLService endpointXmlUtils;

    private XMLMapperService xmlMapper;

    private XmlComponentHistoryDataItem historyDataItem = null;

    private XMLSupportService xmlSupport;

    private File tempMainFile = null;

    private File tempIntegratingFile = null;

    private File resultFile = null;

    private File xsltFile = null;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        xmlSupport = componentContext.getService(XMLSupportService.class);
        xmlMapper = componentContext.getService(XMLMapperService.class);
        endpointXmlUtils = componentContext.getService(EndpointXMLService.class);
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();

        TempFileService tempFileService = TempFileServiceAccess.getInstance();

        String mappingContent;
        String mappingType;

        if (componentContext.getConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME).equals(
            XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED)) {
            mappingContent = componentContext.getConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);
            mappingType = componentContext.getConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME);
            // If no mapping file has been loaded in the XML Merger, content and type are null at this point.
            if (mappingContent == null || mappingContent.isEmpty() || mappingType == null) {
                throw new ComponentException("No mapping file and/or no mapping type defined for XML Merger.");
            }
        } else {
            FileReferenceTD mappingFile = (FileReferenceTD) componentContext.readInput(XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE);
            try {
                File tempmappingFile = tempFileService.createTempFileFromPattern("XMLMappingFile*");

                dataManagementService.copyReferenceToLocalFile(mappingFile.getFileReference(), tempmappingFile,
                    componentContext.getStorageNetworkDestination());
                mappingContent = FileUtils.readFileToString(tempmappingFile);
                if (mappingFile.getFileName().endsWith(XmlMergerComponentConstants.XMLFILEEND)) {
                    mappingType = XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC;
                } else {
                    mappingType = XmlMergerComponentConstants.MAPPINGTYPE_XSLT;
                }
                if (mappingContent == null || mappingContent.isEmpty() || mappingType == null) {
                    throw new ComponentException("No mapping file and/or no mapping type defined for XML Merger.");
                }
            } catch (IOException e) {
                throw new ComponentException("Mapping file from input could not be read.");
            }
        }

        FileReferenceTD mainXML = (FileReferenceTD) componentContext.readInput(XmlMergerComponentConstants.ENDPOINT_NAME_XML);
        FileReferenceTD xmlToIntegrate = (FileReferenceTD) componentContext
            .readInput(XmlMergerComponentConstants.INPUT_NAME_XML_TO_INTEGRATE);

        tempMainFile = null;
        tempIntegratingFile = null;
        resultFile = null;
        xsltFile = null;

        try {
            tempMainFile = tempFileService.createTempFileFromPattern("XMLMerger-*.xml");
            tempIntegratingFile = tempFileService.createTempFileFromPattern("XMLMerger-to-integrate-*.xml");

            dataManagementService.copyReferenceToLocalFile(mainXML.getFileReference(), tempMainFile,
                componentContext.getStorageNetworkDestination());
            dataManagementService.copyReferenceToLocalFile(xmlToIntegrate.getFileReference(), tempIntegratingFile,
                componentContext.getStorageNetworkDestination());
        } catch (IOException e) {
            throw new ComponentException("Failed to write XML file into a temporary file "
                + "(that is required for XML Merger)", e);
        }

        Map<String, TypedDatum> variableInputs = new HashMap<>();
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName) && !inputName.equals(XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE)) {
                variableInputs.put(inputName, componentContext.readInput(inputName));
            }
        }
        if (!variableInputs.isEmpty()) {
            try {
                endpointXmlUtils.updateXMLWithInputs(tempMainFile, variableInputs, componentContext);
            } catch (DataTypeException e) {
                throw new ComponentException("Failed to add dynamic input values to the XML file", e);
            }
            try {
                String xmlVariableIn = dataManagementService.createTaggedReferenceFromLocalFile(componentContext, tempMainFile,
                    XMLComponentConstants.FILENAME);
                if (historyDataItem != null && !variableInputs.isEmpty()) {
                    historyDataItem.setXmlWithVariablesFileReference(xmlVariableIn);
                }
            } catch (IOException e) {
                String errorMessage = "Failed to store XML file with dynamic input values into the data management"
                    + "; it will not be available in the workflow data browser";
                String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(
                    LogFactory.getLog(XmlMergerComponent.class), errorMessage, e);
                componentContext.getLog().componentError(errorMessage, e, errorId);
            }
        }
        if (mappingType.equals(XmlMergerComponentConstants.MAPPINGTYPE_XSLT)) {
            componentContext.getLog().componentInfo("XSL transformation is applied");
            try {
                xsltFile = tempFileService.createTempFileFromPattern("xsltMapping*.xsl");
                resultFile = tempFileService.createTempFileFromPattern("resultXML*.xml");

                final String tempFilePath = tempIntegratingFile.getCanonicalPath().replaceAll("\\\\", "/");
                mappingContent = mappingContent.replaceAll(XmlMergerComponentConstants.INTEGRATING_INPUT_PLACEHOLDER, tempFilePath);
                FileUtils.writeStringToFile(xsltFile, mappingContent);
            } catch (IOException e) {
                throw new ComponentException("Failed to write XSLT mapping file into a temporary file "
                    + "(that is required for XML Merger)", e);
            }

            try {
                xmlMapper.transformXMLFileWithXSLT(tempMainFile, resultFile, xsltFile, new XMLMergerTextLinesReceiver());
            } catch (XMLException e) {
                throw new ComponentException("XSL transformation failed", e);
            }

            componentContext.getLog().componentInfo("XSL transformation successful");
        } else if (mappingType.equals(XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC)) {
            componentContext.getLog().componentInfo("XML mapping is applied");
            resultFile = map(tempMainFile, tempIntegratingFile, mappingContent);

            if (tempMainFile == null || tempIntegratingFile == null || resultFile == null) {
                throw new ComponentException("Something does not perform correct in XML Merger component during classic mapping."
                    + " All files are not filled properly.");
            }
            componentContext.getLog().componentInfo("XML mapping successful");
        }

        try {
            endpointXmlUtils.updateOutputsFromXML(resultFile, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to extract dynamic output values from the merged XML file", e);
        }

        final FileReferenceTD fileReference;
        try {
            fileReference = dataManagementService.createFileReferenceTDFromLocalFile(componentContext, resultFile,
                componentContext.getInstanceName() + XMLComponentConstants.XML_APPENDIX_FILENAME);
        } catch (IOException e) {
            throw new ComponentException("Failed to store merged XML file into the data management - "
                + "if it is not stored in the data management, it can not be sent as output value", e);
        }
        componentContext.writeOutput(XmlMergerComponentConstants.ENDPOINT_NAME_XML, fileReference);

        storeHistoryDataItem();
        deleteTempFiles();
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        storeHistoryDataItem();
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
        try {
            if (tempIntegratingFile != null) {
                tempFileService.disposeManagedTempDirOrFile(tempIntegratingFile);
            }
        } catch (IOException e) {
            LOG.error(FAILED_TO_DELETE_TEMP_FILE + tempIntegratingFile.getAbsolutePath(), e);
        }

        try {
            if (resultFile != null) {
                tempFileService.disposeManagedTempDirOrFile(resultFile);
            }
        } catch (IOException e) {
            LOG.error(FAILED_TO_DELETE_TEMP_FILE + resultFile.getAbsolutePath(), e);
        }

        try {
            if (xsltFile != null) {
                tempFileService.disposeManagedTempDirOrFile(xsltFile);
            }
        } catch (IOException e) {
            LOG.error(FAILED_TO_DELETE_TEMP_FILE + xsltFile.getAbsolutePath(), e);
        }

    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new XmlComponentHistoryDataItem(XmlMergerComponentConstants.COMPONENT_ID);
        }
    }

    private void storeHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    /**
     * Mapping algorithm.
     * 
     * @param main The original XML data set
     * @param integrating The second data set to integrate
     * @param mappingRules TODO
     * @return The combined data set
     * @throws ComponentException Thrown if XML mapping fails.
     */
    private File map(final File main, final File integrating, String mappingRules) throws ComponentException {
        if ((main != null) && (integrating != null)) {
            if (mappingRules == null || mappingRules.equals("null")) {
                throw new ComponentException("Failed to perform mapping as no mapping rules are given. Check the mapping file configured");
            }
            try {
                final Document mappingDoc = xmlSupport.readXMLFromString(mappingRules);
                resultFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("xml*.xml");
                FileUtils.copyFile(main, resultFile);
                xmlMapper.transformXMLFileWithXMLMappingInformation(integrating, resultFile, mappingDoc);

                return resultFile;
            } catch (XPathExpressionException | XMLException | IOException e) {
                throw new ComponentException("Failed to perform XML mapping", e);
            }
        } else {
            return null;
        }
    }
}

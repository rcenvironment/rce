/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.core.utils.incubator.xml.api.XMLMapperConstants;
import de.rcenvironment.core.utils.incubator.xml.api.XMLMapperService;
import de.rcenvironment.core.utils.incubator.xml.api.XMLSupportService;

/**
 * Implementing class for XMLMerger functionality with file support.
 * 
 * @author Markus Kunde
 * @author Miriam Lenk#
 * @author Jan FLink
 * @author Brigitte Boden
 */
public class XmlMergerComponent extends DefaultComponent {

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
        String mappingContent = componentContext.getConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);
        String mappingType = componentContext.getConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME);

        FileReferenceTD mainXML = (FileReferenceTD) componentContext.readInput(XmlMergerComponentConstants.INPUT_NAME_XML);
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
                componentContext.getDefaultStorageNodeId());
            dataManagementService.copyReferenceToLocalFile(xmlToIntegrate.getFileReference(), tempIntegratingFile,
                componentContext.getDefaultStorageNodeId());
        } catch (IOException e) {
            throw new ComponentException("Cannot create temp files for origin XML or XML to integrate: " + e.getMessage());
        }

        Map<String, TypedDatum> variableInputs = new HashMap<>();
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)) {
                variableInputs.put(inputName, componentContext.readInput(inputName));
            }
        }
        if (!variableInputs.isEmpty()) {
            try {
                endpointXmlUtils.updateXMLWithInputs(tempMainFile, variableInputs, componentContext);
            } catch (DataTypeException e2) {
                throw new ComponentException(e2.getMessage(), e2);
            }
            try {
                String xmlVariableIn = dataManagementService.createTaggedReferenceFromLocalFile(componentContext, tempMainFile,
                    XMLComponentConstants.FILENAME);
                if (historyDataItem != null && !variableInputs.isEmpty()) {
                    historyDataItem.setXmlWithVariablesFileReference(xmlVariableIn);
                }
            } catch (IOException e1) {
                throw new ComponentException("Cannot write incoming variables to history browser.", e1);
            }
        }
        if (mappingType.equals(XmlMergerComponentConstants.MAPPINGTYPE_XSLT)) {
            try {
                xsltFile = tempFileService.createTempFileFromPattern("xsltMapping*.xsl");
                resultFile = tempFileService.createTempFileFromPattern("resultXML*.xml");

                final String tempFilePath = tempIntegratingFile.getCanonicalPath().replaceAll("\\\\", "/");
                mappingContent = mappingContent.replaceAll(XmlMergerComponentConstants.INTEGRATING_INPUT_PLACEHOLDER, tempFilePath);
                FileUtils.writeStringToFile(xsltFile, mappingContent);
            } catch (IOException e) {
                throw new ComponentException("Cannot create temp files for xslt mapping file.", e);
            }

            try {
                xmlMapper.transformXMLFileWithXSLT(tempMainFile, resultFile, xsltFile);
            } catch (XMLException e) {
                throw new ComponentException("XSL transformation failed", e);
            }

            if (tempMainFile == null || tempIntegratingFile == null || xsltFile == null || resultFile == null) {
                throw new ComponentException("An error during XSLT mapping occured. Not all files are generated properly.");
            }
        } else if (mappingType.equals(XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC)) {
            resultFile = performMappingWithGlobalMappingLock(tempMainFile, tempIntegratingFile);

            if (tempMainFile == null || tempIntegratingFile == null || resultFile == null) {
                throw new ComponentException(
                    "Something does not perform correct in XML Merger component during classic mapping."
                        + " All files are not filled properly.");
            }
        }

        try {
            endpointXmlUtils.updateOutputsFromXML(resultFile, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException(e.getMessage(), e);
        }

        // Output write
        final FileReferenceTD fileReference;
        try {
            fileReference =
                dataManagementService.createFileReferenceTDFromLocalFile(componentContext, resultFile,
                    componentContext.getInstanceName() + XMLComponentConstants.XML_APPENDIX_FILENAME);

            componentContext.writeOutput(XmlMergerComponentConstants.INPUT_NAME_XML, fileReference);

        } catch (IOException e) {
            throw new ComponentException("Cannot write XML to output channel in XML Merger component.");
        }

        storeHistoryDataItem();
        deleteTempFiles();
    }

    @Override
    public void tearDown(FinalComponentState state) {
        if (state == FinalComponentState.FAILED) {
            storeHistoryDataItem();
            deleteTempFiles();
        }
    }

    private void deleteTempFiles() {
        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        try {
            if (tempMainFile != null) {
                tempFileService.disposeManagedTempDirOrFile(tempMainFile);
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).warn(FAILED_TO_DELETE_TEMP_FILE + tempMainFile.getAbsolutePath(), e);
        }
        try {
            if (tempIntegratingFile != null) {
                tempFileService.disposeManagedTempDirOrFile(tempIntegratingFile);
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).warn(FAILED_TO_DELETE_TEMP_FILE + tempIntegratingFile.getAbsolutePath(), e);
        }

        try {
            if (resultFile != null) {
                tempFileService.disposeManagedTempDirOrFile(resultFile);
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).warn(FAILED_TO_DELETE_TEMP_FILE + resultFile.getAbsolutePath(), e);
        }

        try {
            if (xsltFile != null) {
                tempFileService.disposeManagedTempDirOrFile(xsltFile);
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).warn(FAILED_TO_DELETE_TEMP_FILE + xsltFile.getAbsolutePath(), e);
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
     * @return The combined data set
     * @throws ComponentException Thrown if XML mapping fails.
     */
    private File map(final File main, final File integrating) throws ComponentException {
        LOG.debug("Mapping in merger component.");
        if ((main != null) && (integrating != null)) {
            LOG.debug("Using classic output mapping");
            // Mapping document
            final String mappingRules = componentContext.getConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);
            if (mappingRules == null || mappingRules.equals("null")) {
                throw new ComponentException("Mapping cannot be done because mapping rules are null");
            }
            try {
                final Document mappingDoc = xmlSupport.readXMLFromString(mappingRules);
                final Document updatingDoc = xmlSupport.readXMLFromFile(integrating);
                final Document cpacsInOut = xmlSupport.readXMLFromFile(main);
                xmlMapper.transformXMLFileWithXMLMappingInformation(updatingDoc, cpacsInOut, mappingDoc);
                resultFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("xml*.xml");
                xmlSupport.writeXMLtoFile(cpacsInOut, resultFile);
                return resultFile;
            } catch (XPathExpressionException | XMLException | IOException e) {
                throw new ComponentException("Error while processing XML mapping", e);
            }
        } else {
            return null;
        }
    }
    
    private File performMappingWithGlobalMappingLock(final File main, final File integrating) throws ComponentException {
        synchronized (XMLMapperConstants.GLOBAL_MAPPING_LOCK) {
            return map(main, integrating);            
        }
    }
}

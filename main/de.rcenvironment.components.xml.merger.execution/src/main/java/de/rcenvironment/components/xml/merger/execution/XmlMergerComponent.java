/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
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
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.incubator.XMLHelper;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;
import de.rcenvironment.cpacs.utils.common.components.XmlComponentHistoryDataItem;
import de.rcenvironment.cpacs.utils.common.xml.ComponentVariableMapper;
import de.rcenvironment.cpacs.utils.common.xml.XMLMapper;
import de.rcenvironment.cpacs.utils.common.xml.XMLMappingInformation;

/**
 * Implementing class for XMLMerger functionality with file support.
 * 
 * @author Markus Kunde
 * @author Miriam Lenk#
 * @author Jan FLink
 */
public class XmlMergerComponent extends DefaultComponent {

    private static final String FAILED_TO_DELETE_TEMP_FILE = "Failed to delete temp file: ";

    private static final Log LOG = LogFactory.getLog(XmlMergerComponent.class);
    
    private ComponentContext componentContext;
    
    private ComponentDataManagementService dataManagementService;

    private XmlComponentHistoryDataItem historyDataItem = null;

    private XMLHelper xmlHelper = new XMLHelper();

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
    }
    
    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();
        
        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        final ComponentVariableMapper varMapper = new ComponentVariableMapper();

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
                varMapper.updateXMLWithInputs(tempMainFile.getAbsolutePath(), variableInputs, componentContext);
            } catch (DataTypeException e2) {
                throw new ComponentException(e2.getMessage(), e2);
            }
            try {
                String xmlVariableIn = dataManagementService.createTaggedReferenceFromLocalFile(componentContext, tempMainFile,
                        ChameleonCommonConstants.CHAMELEON_CPACS_FILENAME);
                if (historyDataItem != null && !variableInputs.isEmpty()) {
                    historyDataItem.setXmlWithVariablesFileReference(xmlVariableIn);
                }
            } catch (IOException e1) {
                LOG.debug("Cannot write incoming variables to history browser.");
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
                throw new ComponentException("Cannot create temp files for xslt mapping file.");
            }

            if (!transformXSLT(
                tempMainFile.getAbsolutePath(),
                xsltFile.getAbsolutePath(),
                resultFile.getAbsolutePath())) {
                throw new ComponentException("XSL-Transformation in XML Merger component fails.");
            }

            if (tempMainFile == null || tempIntegratingFile == null || xsltFile == null || resultFile == null) {
                throw new ComponentException("An error during XSLT mapping occured. Not all files are generated properly.");
            }
        } else if (mappingType.equals(XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC)) {
            resultFile = mapping(tempMainFile, tempIntegratingFile);

            if (tempMainFile == null || tempIntegratingFile == null || resultFile == null) {
                throw new ComponentException(
                    "Something does not perform correct in XML Merger component during classic mapping."
                        + " All files are not filled properly.");
            }
        }

        try {
            varMapper.updateOutputsFromXML(resultFile.getAbsolutePath(), XmlMergerComponentConstants.INPUT_NAME_XML, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException(e.getMessage(), e);
        }

        // Output write
        final FileReferenceTD fileReference;
        try {
            fileReference =
                dataManagementService.createFileReferenceTDFromLocalFile(componentContext, resultFile,
                    componentContext.getInstanceName() + ChameleonCommonConstants.XML_APPENDIX_FILENAME);

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
    private File mapping(final File main, final File integrating) throws ComponentException {
        LOG.debug("mapping in aggregator component.");
        final XMLMapper xmlMapper = new XMLMapper();
        if ((main != null) && (integrating != null)) {
            LOG.debug("Using classic output mapping");
            // Mapping document
            final String mappingRules = componentContext.getConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);

            try {
                final Document mappingDoc = xmlHelper.readXMLFromString(mappingRules);
                final Document updatingDoc = xmlHelper.readXMLFromFile(integrating);
                final List<XMLMappingInformation> mappings = xmlMapper.readXMLMapping(mappingDoc);
                final Document cpacsInOut = xmlHelper.readXMLFromFile(main);
                xmlMapper.map(updatingDoc, cpacsInOut, mappings);
                resultFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("xml*.xml");
                xmlHelper.writeXML(cpacsInOut, resultFile.getAbsolutePath());
                return resultFile;
            } catch (XPathExpressionException | XMLException | IOException e) {
                throw new ComponentException("Error while processing XML mapping", e);
            }
        } else {
            return null;
        }
    }

    /**
     * Helper for the XML transformation.
     * 
     * @param fileNameXml The file to transform
     * @param fileNameXslt The transformation prescription
     * @param fileNameResult The file of results
     * @return True if successful
     */
    private boolean transformXSLT(final String fileNameXml, final String fileNameXslt, final String fileNameResult) {
        final TransformerFactory tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        Transformer transformer = null;

        try {
            OutputStream outputStream = new FileOutputStream(fileNameResult);
            transformer = tFactory.newTransformer(new StreamSource(fileNameXslt));
            transformer.transform(new StreamSource(fileNameXml), new StreamResult(outputStream));
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (final TransformerConfigurationException e) {
            LOG.error(e);
        } catch (final FileNotFoundException e) {
            LOG.error(e);
        } catch (final TransformerException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        }
        return false;
    }

}

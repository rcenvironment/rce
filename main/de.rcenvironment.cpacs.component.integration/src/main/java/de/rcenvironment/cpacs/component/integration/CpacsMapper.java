/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.component.integration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.utils.incubator.XMLHelper;
import de.rcenvironment.core.utils.incubator.XSLTErrorHandler;
import de.rcenvironment.core.utils.incubator.XSLTransformer;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.cpacs.utils.common.xml.XMLMapper;
import de.rcenvironment.cpacs.utils.common.xml.XMLMappingInformation;

/**
 * Input mapping, tool specific mapping and output mapping for integrated cpacs tools.
 * 
 * @author Jan Flink
 * @author Markus Kunde
 */
public class CpacsMapper {

    private static final String STRING_CPACS_RESULT_FILE_CREATED = "CPACS result file created (%s)): %s";

    private static final String STRING_TOOL_OUTPUT_FILE_EXISTS = "Tool output file exists: %s";

    private static final String SUFFIX_MAPPED = "-mapped";

    private static final String STRING_TOOL_INPUT_FILE_NOT_FOUND = "Tool input file '%s' not found.";

    private static final String STRING_XML_ERROR_DURING_MAPPING = "XML error during %s mapping.";

    private static final String STRING_TOOL_INPUT_CREATED = "Tool input file created (%s)): %s";

    private static final String STRING_MAPPING_USAGE = "%s: Use %s %s mapping";

    private static final String STRING_MAPPING_TYPE_XML = "pairing";

    private static final String STRING_MAPPING_TYPE_XSL = "raw XSLT";

    private static final String STRING_MAPPING_DIRECTION_INPUT = "input";

    private static final String STRING_MAPPING_DIRECTION_OUTPUT = "output";

    private static final String STRING_MAPPING_DIRECTION_TOOLSPECIFIC = "tool sepecific input";

    private static final String STRING_MAPPING_FILE_NOT_FOUND = "Mapping file '%s' not found.";

    private static final String STRING_ERROR_SOLVING_FILE_EXTENSION = "Error solving file extension of mapping file '%s'.";

    private static final String CREATE_MAPPING_XSLT_FILEPATH = "/resources/CreateMapping.xslt";

    private static final Log LOGGER = LogFactory.getLog(CpacsMapper.class);

    private static final String XMLFILE_SEPARATOR = "/";

    /** Helper for processing the mapping rules and doing the mapping. */
    private XMLMapper xmlMapper = new XMLMapper();

    private XMLHelper xmlHelper = new XMLHelper();

    /** Helper for processing transformation and mapping on xml-files. */
    private XSLTransformer xmlTransformer = new XSLTransformer();

    /** Concrete tool. */
    private CpacsToolIntegratorComponent cpacsTool;

    public CpacsMapper(CpacsToolIntegratorComponent ct) {
        cpacsTool = ct;
    }

    /**
     * Mapping tool input side.
     * 
     * @param cpacsInitial The initial CPACS file
     * @throws ComponentException Error during input mapping.
     * 
     */
    public void mapInput(String cpacsInitial) throws ComponentException {
        final File mappingFile = new File(cpacsTool.getInputMapping());

        if (mappingFile.exists()) {
            if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                LOGGER.debug(String.format(STRING_MAPPING_USAGE, cpacsTool.getToolName(), STRING_MAPPING_TYPE_XML,
                    STRING_MAPPING_DIRECTION_INPUT));
                try {
                    Document mappingDoc;
                    mappingDoc = transformXMLMapping(mappingFile.getAbsolutePath(),
                        cpacsInitial,
                        StringUtils.EMPTY);
                    final List<XMLMappingInformation> mappings = xmlMapper.readXMLMapping(mappingDoc);
                    // Build tool input document
                    final Document myToolDoc = xmlHelper.createDocument();
                    Document cpacsIncoming = xmlHelper.readXMLFromFile(new File(cpacsInitial));
                    xmlMapper.map(cpacsIncoming, myToolDoc, mappings);
                    String toolInputFilePath = cpacsTool.getToolInput();
                    xmlHelper.writeXML(myToolDoc, toolInputFilePath);
                    LOGGER.debug(String.format(STRING_TOOL_INPUT_CREATED,
                        toolInputFilePath, String.valueOf(new File(toolInputFilePath).exists())));
                } catch (XPathExpressionException | XMLException e) {
                    throw new ComponentException(String.format(STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_INPUT), e);
                }
            } else if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                LOGGER.debug(String.format(STRING_MAPPING_USAGE, cpacsTool.getToolName(), STRING_MAPPING_TYPE_XSL,
                    STRING_MAPPING_DIRECTION_INPUT));
                try {
                    transformXSLT(cpacsInitial, mappingFile.getAbsolutePath(), cpacsTool.getToolInput());
                } catch (XMLException e) {
                    throw new ComponentException(String.format(STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_INPUT), e);
                }
            } else {
                throw new ComponentException(String.format(
                    STRING_ERROR_SOLVING_FILE_EXTENSION, cpacsTool.getInputMapping()));
            }
        } else {
            throw new ComponentException(String.format(STRING_MAPPING_FILE_NOT_FOUND, cpacsTool.getInputMapping()));
        }
    }

    /**
     * Map the toolSpecificInput file into the existing toolinput-file.
     * 
     * @throws ComponentException Thrown if tool specific input mapping fails.
     */
    public void mergeToolspecificInput() throws ComponentException {
        final File mappingFile = new File(cpacsTool.getToolspecificInputMapping());

        if (mappingFile.exists()) {
            if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                LOGGER.debug(String.format(STRING_MAPPING_USAGE, cpacsTool.getToolName(), STRING_MAPPING_TYPE_XML,
                    STRING_MAPPING_DIRECTION_TOOLSPECIFIC));

                Document mappedDoc;
                try {
                    mappedDoc = xmlHelper.readXMLFromFile(cpacsTool.getToolInput());
                    final Document toolDoc = xmlHelper.readXMLFromFile(cpacsTool.getToolspecificInputData());
                    final Document mappingDoc = transformXMLMapping(
                        cpacsTool.getToolspecificInputMapping(),
                        cpacsTool.getToolspecificInputData(),
                        cpacsTool.getToolInput());
                    final List<XMLMappingInformation> mappings = xmlMapper.readXMLMapping(mappingDoc);

                    xmlMapper.map(toolDoc, mappedDoc, mappings);

                    // overwrite old tool input file
                    xmlHelper.writeXML(mappedDoc, cpacsTool.getToolInput());
                } catch (XPathExpressionException | XMLException e) {
                    throw new ComponentException(String.format(STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_TOOLSPECIFIC), e);
                }
            } else if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                LOGGER.debug(String.format(STRING_MAPPING_USAGE, cpacsTool.getToolName(), STRING_MAPPING_TYPE_XSL,
                    STRING_MAPPING_DIRECTION_TOOLSPECIFIC));
                File toolInputMapped = new File(cpacsTool.getToolInput() + SUFFIX_MAPPED);
                File toolInput = new File(cpacsTool.getToolInput());
                try {
                    FileUtils.copyFile(toolInput, toolInputMapped, true);
                    xmlTransformer.transformXMLFile(
                        cpacsTool.getToolInput() + SUFFIX_MAPPED,
                        cpacsTool.getToolspecificInputMapping(),
                        cpacsTool.getToolInput());
                } catch (IOException e) {
                    throw new ComponentException(String.format(STRING_TOOL_INPUT_FILE_NOT_FOUND, cpacsTool.getToolInput()), e);
                } catch (XMLException e) {
                    throw new ComponentException(String.format(STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_TOOLSPECIFIC));
                }

            } else {
                throw new ComponentException(String.format(
                    STRING_ERROR_SOLVING_FILE_EXTENSION,
                    cpacsTool.getToolspecificInputMapping()));
            }
        } else {
            throw new ComponentException(String.format(STRING_MAPPING_FILE_NOT_FOUND, cpacsTool.getToolspecificInputMapping()));
        }
    }

    /**
     * Mapping tool output side. *
     * 
     * @param cpacsInitial The initial CPACS file
     * @throws ComponentException Thrown if tool output mapping fails.
     * 
     */
    public void mapOutput(String cpacsInitial) throws ComponentException {
        if (new File(cpacsTool.getToolOutput()).exists()) {
            LOGGER.debug(String.format(STRING_TOOL_OUTPUT_FILE_EXISTS, new File(cpacsTool.getToolOutput()).exists()));

            final File mappingFile = new File(cpacsTool.getOutputMapping());

            if (mappingFile.exists()) {
                if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XML)) {
                    LOGGER.debug(String.format(STRING_MAPPING_USAGE, cpacsTool.getToolName(), STRING_MAPPING_TYPE_XML,
                        STRING_MAPPING_DIRECTION_OUTPUT));
                    try {
                        Document toolDoc;
                        toolDoc = xmlHelper.readXMLFromFile(cpacsTool.getToolOutput());
                        // Build CPACS-Result-File through mapping
                        final Document mappingDoc = transformXMLMapping(
                            mappingFile.getAbsolutePath(),
                            cpacsTool.getToolOutput(),
                            cpacsInitial);
                        final List<XMLMappingInformation> mappings = xmlMapper.readXMLMapping(mappingDoc);

                        Document cpacsInOut = xmlHelper.readXMLFromFile(new File(cpacsInitial));

                        xmlMapper.map(toolDoc, cpacsInOut, mappings);

                        // Update CPACS-File
                        String cpacsResultFilePath = cpacsTool.getCpacsResult();
                        xmlHelper.writeXML(cpacsInOut, cpacsResultFilePath);
                        LOGGER.debug(String.format(STRING_CPACS_RESULT_FILE_CREATED,
                            cpacsResultFilePath, String.valueOf(new File(cpacsResultFilePath).exists())));
                    } catch (XPathExpressionException | XMLException e) {
                        throw new ComponentException(String.format(STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_OUTPUT), e);
                    }

                } else if (mappingFile.getName().endsWith(CpacsToolIntegrationConstants.FILE_SUFFIX_XSL)) {
                    LOGGER.debug(String.format(STRING_MAPPING_USAGE, cpacsTool.getToolName(), STRING_MAPPING_TYPE_XSL,
                        STRING_MAPPING_DIRECTION_OUTPUT));
                    try {
                        transformXSLT(cpacsInitial, mappingFile.getAbsolutePath(), cpacsTool.getCpacsResult());
                    } catch (XMLException e) {
                        throw new ComponentException(String.format(STRING_XML_ERROR_DURING_MAPPING, STRING_MAPPING_DIRECTION_OUTPUT), e);
                    }
                } else {
                    throw new ComponentException(String.format(
                        STRING_ERROR_SOLVING_FILE_EXTENSION, cpacsTool.getOutputMapping()));
                }
            } else {
                throw new ComponentException(String.format(STRING_MAPPING_FILE_NOT_FOUND, cpacsTool.getOutputMapping()));
            }
        }
    }

    /**
     * Helper for the XML transformation.
     * 
     * @param fileNameXml The file to transform
     * @param fileNameXslt The transformation prescription
     * @param fileNameResult The file of results
     * @return True if successful
     * @throws ComponentException Thrown if XSLT transformation fails.
     */
    private void transformXSLT(final String fileNameXml, final String fileNameXslt, final String fileNameResult)
        throws XMLException {
        final TransformerFactory tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        Transformer transformer = null;

        try {
            OutputStream outputStream = new FileOutputStream(fileNameResult);
            transformer = tFactory.newTransformer(new StreamSource(fileNameXslt));
            transformer.transform(new StreamSource(fileNameXml), new StreamResult(outputStream));
            outputStream.flush();
            outputStream.close();
        } catch (TransformerException | IOException e) {
            throw new XMLException("XSL-Transformation fails.", e);
        }
    }

    /**
     * Transforms a XML mapping stylesheet to the final mapping XML document by surrounding it with a XSLT header and executing this XSLT
     * stylesheet. This method rearranges, e.g., xslt loops to simple source/target mappings.
     * 
     * @param mappingFilename The file name of the mapping file to be transformed.
     * @param sourceFilename The file name of an input (source) file.
     * @param targetFilename The file name of the target file, in which the source file should be imported. If content is empty ("") a new
     *        file will be created.
     * 
     * @return Returns the final mapping XML document as DOM document.
     * @throws ComponentException Thrown if mapping fails.
     */
    private Document transformXMLMapping(final String mappingFilename, final String sourceFilename, final String targetFilename)
        throws XMLException {
        try {
            final TransformerFactory transformerFac = TransformerFactory.newInstance();
            transformerFac.setErrorListener(new XSLTErrorHandler());

            // First read in the mapping XML file and transform it to a valid
            // XSLT stylesheet by surrounding it with the appropiate stylesheet elements.
            // This is done via the stylesheet CreateMapping.xslt which is loaded from
            // the jar file or the package path.
            final InputStream inStream = this.getClass().getResourceAsStream(CREATE_MAPPING_XSLT_FILEPATH);
            final Transformer transformer1 = transformerFac.newTransformer(new StreamSource(inStream));
            transformer1.setErrorListener(new XSLTErrorHandler());
            final DOMSource mappingSrc = new DOMSource(xmlHelper.readXMLFromFile(mappingFilename));
            final Document tempDoc = xmlHelper.createDocument();
            final DOMResult tempXSLT = new DOMResult(tempDoc);
            transformer1.transform(mappingSrc, tempXSLT);
            // Now transform the resulting mapping XSLT to the final mapping file which
            // only contains mapping elements and no more xsl elements like loops, conditions etc.
            final DOMSource sourceXSLT = new DOMSource(tempDoc);
            final Transformer transformer2 = transformerFac.newTransformer(sourceXSLT);
            transformer2.setErrorListener(new XSLTErrorHandler());

            transformer2.setParameter("sourceFilename", sourceFilename.replace("\\", XMLFILE_SEPARATOR));
            transformer2.setParameter("targetFilename", targetFilename.replace("\\", XMLFILE_SEPARATOR));

            final DOMSource source = new DOMSource(xmlHelper.createDocument());
            final Document resultDoc = xmlHelper.createDocument();
            final DOMResult result = new DOMResult(resultDoc);
            transformer2.transform(source, result);

            return resultDoc;
        } catch (final NullPointerException | TransformerException | XMLException e) {
            throw new XMLException("XML-Transformation fails.", e);
        }
    }

}

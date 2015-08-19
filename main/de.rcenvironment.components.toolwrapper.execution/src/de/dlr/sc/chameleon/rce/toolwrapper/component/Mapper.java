/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.utils.incubator.XSLTErrorHandler;
import de.rcenvironment.core.utils.incubator.XSLTransformer;
import de.rcenvironment.core.utils.incubator.XMLHelper;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.cpacs.utils.common.xml.XMLMapper;
import de.rcenvironment.cpacs.utils.common.xml.XMLMappingInformation;

/**
 * Mapping.
 * 
 * @author Markus Kunde
 * @author Jan Flink
 */
@Deprecated
public class Mapper {

    private static final String MAPPING_XSLT = "/resources/CreateMapping.xslt";

    private static final Log LOGGER = LogFactory.getLog(Mapper.class);

    private static final String XMLFILE_SEPARATOR = "/";

    /** Helper for processing the mapping rules and doing the mapping. */
    private XMLMapper xmlMapper = new XMLMapper();

    private XMLHelper xmlHelper = new XMLHelper();

    /** Helper for processing transformation and mapping on xml-files. */
    private XSLTransformer xmlTransformer = new XSLTransformer();

    /** Concrete tool. */
    private CpacsTool tool;

    public Mapper(CpacsTool ct) {
        tool = ct;
    }

    /**
     * Mapping tool input side.
     * 
     * @throws ComponentException Error during input mapping.
     * 
     */
    public void mappingInput() throws ComponentException {
        final File rawXsltFile = new File(tool.toolCPACSBehaviorConfiguration.getInputMappingRawXslt());

        if (rawXsltFile.exists()) {
            LOGGER.debug("Use raw XSLT input mapping");
            if (!transformXSLT(tool.toolCPACSBehaviorConfiguration.getCpacsInitial(),
                tool.toolCPACSBehaviorConfiguration.getInputMappingRawXslt(),
                tool.toolCPACSBehaviorConfiguration.getToolInput())) {
                throw new ComponentException("XSL-Transformation on toolspecific input-side fails.");
            }
        } else if (new File(tool.toolCPACSBehaviorConfiguration.getInputMapping()).exists()) {
            LOGGER.debug("Use pairing input mapping");
            try {
                Document mappingDoc;
                mappingDoc = transformXMLMapping(tool.toolCPACSBehaviorConfiguration.getInputMapping(),
                    tool.toolCPACSBehaviorConfiguration.getCpacsInitial(),
                    StringUtils.EMPTY);
                final List<XMLMappingInformation> mappings = xmlMapper.readXMLMapping(mappingDoc);
                // Build tool export document
                LOGGER.debug("Building tool export XML");
                final Document myToolDoc = xmlHelper.createDocument();
                Document cpacsIncoming = xmlHelper.readXMLFromFile(new File(tool.toolCPACSBehaviorConfiguration.getCpacsInitial()));
                xmlMapper.map(cpacsIncoming, myToolDoc, mappings);
                String toolInputFilePath = tool.toolCPACSBehaviorConfiguration.getToolInput();
                xmlHelper.writeXML(myToolDoc, toolInputFilePath);
                LOGGER.debug(String.format("Tool input file created (%s)): %s",
                    toolInputFilePath, String.valueOf(new File(toolInputFilePath).exists())));
            } catch (XMLException | XPathExpressionException e) {
                throw new ComponentException("XML error during input mapping.", e);
            }
        } else {
            throw new ComponentException("Cannot discover any input mapping rules.");
        }

        if (tool.toolConfiguration.hasToolspecificinputfile()) {
            mergeToolspecificInput();
        }
    }

    /**
     * Map the toolSpecificInput file into the existing toolinput-file.
     * 
     * @throws ComponentException Error in tool specific input mapping.
     */
    private void mergeToolspecificInput() throws ComponentException {
        final File rawXsltFile = new File(tool.toolCPACSBehaviorConfiguration.getToolspecificInputMappingRawXslt());

        if (rawXsltFile.exists()) {
            File toolInputMapped = new File(tool.toolCPACSBehaviorConfiguration.getToolInput() + "-mapped");
            File toolInput = new File(tool.toolCPACSBehaviorConfiguration.getToolInput());
            try {
                FileUtils.copyFile(toolInput, toolInputMapped, true);
                xmlTransformer.transformXMLFile(
                    tool.toolCPACSBehaviorConfiguration.getToolInput() + "-mapped",
                    tool.toolCPACSBehaviorConfiguration.getToolspecificInputMappingRawXslt(),
                    tool.toolCPACSBehaviorConfiguration.getToolInput());
            } catch (IOException e) {
                throw new ComponentException("Tool input file not found.", e);
            } catch (XMLException e) {
                throw new ComponentException("XSL-Transformation on toolspecific input-side fails.");
            }
        } else if (new File(tool.toolCPACSBehaviorConfiguration.getToolspecificInputMapping()).exists()) {
            LOGGER.debug("Use pairing tool specific input mapping");

            Document mappedDoc;
            try {
                mappedDoc = xmlHelper.readXMLFromFile(tool.toolCPACSBehaviorConfiguration.getToolInput());
                final Document toolDoc = xmlHelper.readXMLFromFile(tool.toolCPACSBehaviorConfiguration.getToolspecificInputData());
                final Document mappingDoc = transformXMLMapping(
                    tool.toolCPACSBehaviorConfiguration.getToolspecificInputMapping(),
                    tool.toolCPACSBehaviorConfiguration.getToolspecificInputData(),
                    tool.toolCPACSBehaviorConfiguration.getToolInput());
                final List<XMLMappingInformation> mappings = xmlMapper.readXMLMapping(mappingDoc);

                xmlMapper.map(toolDoc, mappedDoc, mappings);

                // overwrite old tool input file
                xmlHelper.writeXML(mappedDoc, tool.toolCPACSBehaviorConfiguration.getToolInput());
            } catch (XPathExpressionException | XMLException e) {
                throw new ComponentException("XML error during tool specific input mapping", e);
            }
        } else {
            throw new ComponentException("Cannot discover any tool specific input mapping rules.");
        }
    }

    /**
     * Mapping tool output side.
     * 
     * @throws ComponentException Thrown if tool output mapping fails.
     * 
     */
    public void mappingOutput() throws ComponentException {
        if (new File(tool.toolCPACSBehaviorConfiguration.getOutputDir()).exists()) {
            final File rawXsltFile = new File(tool.toolCPACSBehaviorConfiguration.getOutputMappingRawXslt());

            if (rawXsltFile.exists()) {
                // True = New raw XSLT transformation
                LOGGER.debug("Use raw XSLT output mapping");

                if (!transformXSLT(
                    tool.toolCPACSBehaviorConfiguration.getCpacsInitial(),
                    tool.toolCPACSBehaviorConfiguration.getOutputMappingRawXslt(),
                    tool.toolCPACSBehaviorConfiguration.getCpacsResult())) {
                    throw new ComponentException("XSL-Transformation on output-side fails.");
                }
            } else if (new File(tool.toolCPACSBehaviorConfiguration.getOutputMapping()).exists()) {
                // False = old transformation style
                LOGGER.debug("Use pairing output mapping");
                LOGGER.debug("Tool output file exists: " + new File(tool.toolCPACSBehaviorConfiguration.getToolOutput()).exists());
                // Build CPACS-Result-File through mapping
                try {
                    Document mappingDoc;
                    mappingDoc = transformXMLMapping(
                        tool.toolCPACSBehaviorConfiguration.getOutputMapping(),
                        tool.toolCPACSBehaviorConfiguration.getToolOutput(),
                        tool.toolCPACSBehaviorConfiguration.getCpacsInitial());
                    final Document toolDoc = xmlHelper.readXMLFromFile(tool.toolCPACSBehaviorConfiguration.getToolOutput());
                    final List<XMLMappingInformation> mappings = xmlMapper.readXMLMapping(mappingDoc);

                    Document cpacsInOut = xmlHelper.readXMLFromFile(new File(tool.toolCPACSBehaviorConfiguration.getCpacsInitial()));

                    xmlMapper.map(toolDoc, cpacsInOut, mappings);

                    // Update CPACS-File
                    String cpacsResultFilePath = tool.toolCPACSBehaviorConfiguration.getCpacsResult();
                    xmlHelper.writeXML(cpacsInOut, cpacsResultFilePath);
                    LOGGER.debug(String.format("CPACS result file created (%s)): %s",
                        cpacsResultFilePath, String.valueOf(new File(cpacsResultFilePath).exists())));
                } catch (XPathExpressionException | XMLException e) {
                    throw new ComponentException("XML error during output mapping", e);
                }
            } else {
                throw new ComponentException("Cannot discover any output mapping rules.");
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
            LOGGER.error(e);
        } catch (final FileNotFoundException e) {
            LOGGER.error(e);
        } catch (final TransformerException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return false;
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
            final InputStream inStream = this.getClass().getResourceAsStream(MAPPING_XSLT);
            final Transformer transformer1 = transformerFac.newTransformer(new StreamSource(inStream));
            transformer1.setErrorListener(new XSLTErrorHandler());
            final DOMSource mappingSrc = new DOMSource(xmlHelper.readXMLFromFile(mappingFilename));
            final Document tempDoc = xmlHelper.createDocument();
            final DOMResult tempXSLT = new DOMResult(tempDoc);
            transformer1.transform(mappingSrc, tempXSLT);
            // if (System.getProperties().containsKey(MAPPING_PROPERTY_NAME) // TODO untested code
            // && System.getProperty(MAPPING_PROPERTY_NAME).equalsIgnoreCase("true")) {
            // final Document tmpDocument = createDocument();
            // tmpDocument.importNode(tempXSLT.getNode(), true);
            // writeXML(createElement(tmpDocument, "tmp_step1.xslt"));
            // }

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
        } catch (final TransformerConfigurationException tce) {
            throw new XMLException("Transformer factory error", tce);
        } catch (final TransformerException te) {
            throw new XMLException("Transformation error", te);
        } catch (XMLException e) {
            throw new XMLException("Error reading XML from file", e);
        }
    }

}

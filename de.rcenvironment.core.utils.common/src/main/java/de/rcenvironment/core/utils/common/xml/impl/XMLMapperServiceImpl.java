/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.xml.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.rcenvironment.core.utils.common.xml.EMappingMode;
import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.core.utils.common.xml.XMLMapperConstants;
import de.rcenvironment.core.utils.common.xml.XMLMappingInformation;
import de.rcenvironment.core.utils.common.xml.XMLNamespaceContext;
import de.rcenvironment.core.utils.common.xml.XSLTErrorHandler;
import de.rcenvironment.core.utils.common.xml.api.XMLMapperService;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;
import net.sf.saxon.serialize.MessageEmitter;

/**
 * Default implementation of the XML Mapper.
 * 
 * @author Brigitte Boden
 * @author Markus Litz, Markus Kunde, Arne Bachmann (some code adapted from old class XMLHelper)
 */
public class XMLMapperServiceImpl implements XMLMapperService {

    /**
     * XPath delimiter string (slash instead of backslash).
     */
    public static final String XPATH_DELIMITER = "/";

    /**
     * TransformerFactory for XSL-Transformation.
     */
    private TransformerFactory tFactory = null;

    private XMLSupportService xmlSupport;

    private Log log = LogFactory.getLog(getClass());

    public XMLMapperServiceImpl() {

        tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        tFactory.setErrorListener(new XSLTErrorHandler());
    }

    /**
     * OSGI binding method.
     * 
     * @param service The service.
     */
    public void bindXMLSupportService(XMLSupportService service) {
        xmlSupport = service;
    }

    @Override
    public void transformXMLFileWithXSLT(File sourceFile, File resultFile, File xsltFile, TextLinesReceiver logReceiver)
        throws XMLException {
        Transformer transformer = null;
        try (FileOutputStream resultFileOutputStream = new FileOutputStream(resultFile)) {
            transformer = tFactory.newTransformer(new StreamSource(xsltFile));
            transformer.setErrorListener(new XSLTErrorHandler());
            MessageEmitter em = new MessageEmitter();
            // PipedReader reader = new PipedReader();
            StringWriter writer = new StringWriter();
            em.setWriter(writer);
            ((net.sf.saxon.Controller) transformer).setMessageEmitter(em);
            synchronized (XMLMapperConstants.GLOBAL_MAPPING_LOCK) {

                StreamSource streamSource = new StreamSource(sourceFile);
                transformer.transform(streamSource, new StreamResult(resultFileOutputStream));
                em.close();
                writer.close();
            }
            resultFileOutputStream.close();
            StringReader reader = new StringReader(writer.toString());
            LineIterator it = IOUtils.lineIterator(reader);
            while (it.hasNext()) {
                String line = it.nextLine();
                if (logReceiver != null) {
                    logReceiver.addLine("XSL:Message:" + line);
                }
                log.info("XSL:Message:" + line);
            }
            reader.close();
        } catch (TransformerException | IOException e) {
            throw new XMLException(e);
        }
    }

    /**
     * Does the mapping between the elements of a source document and a target document. (adapted from old XMLMapper) Set to protected to
     * reduce usage of Document instances in components to enable global locking.
     * 
     * @param sourceDoc The source document whose elements should be mapped.
     * @param targetDoc The target document.
     * @param mappingsDoc A document with a list of mapping rules.
     * @throws XPathExpressionException Thrown if XPath could not be evaluated.
     * @throws XMLException Mapping error.
     * 
     */
    protected void transformXMLFileWithXMLMappingInformation(Document sourceDoc, Document targetDoc, Document mappingsDoc)
        throws XPathExpressionException, XMLException {
        List<XMLMappingInformation> mappings = readXMLMapping(mappingsDoc);

        // XPath object for querying the source document
        final XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new XMLNamespaceContext(sourceDoc));

        // Loop over all mapping rules
        for (XMLMappingInformation mapInfo : mappings) {
            final EMappingMode mappingMode = mapInfo.getMode();
            final String sourceXPath = mapInfo.getSourceXPath();
            final String targetXPath = mapInfo.getTargetXPath();

            if (mappingMode == EMappingMode.DeleteOnly) {
                xmlSupport.deleteElement(targetDoc, targetXPath);
                continue;
            }

            final NodeList sourceNodes = (NodeList) xpath.evaluate(sourceXPath, sourceDoc, XPathConstants.NODESET);
            if (sourceNodes.getLength() == 0) {
                throw new XMLException("No source elements found for map:source='" + sourceXPath);
            }

            switch (mappingMode) {
            case Append:
                Node sourceNode = sourceNodes.item(0);
                Element sourceElement = null;
                if (sourceNode.getNodeType() == Node.ELEMENT_NODE) {
                    sourceElement = (Element) sourceNode;
                } else if (sourceNode.getNodeType() == Node.DOCUMENT_NODE) {
                    sourceElement = sourceDoc.getDocumentElement();
                }
                if (sourceElement == null) {
                    //Never expected to happen
                    throw new XMLException("Source node has an unsupported type.");
                }
                
                final Document mappingDoc = createXPathMappings(sourceElement, mapInfo);
                transformXMLFileWithXMLMappingInformation(sourceDoc, targetDoc, mappingDoc);
                continue;
            case Delete:
                xmlSupport.deleteElement(targetDoc, targetXPath);
                break;
            default:
                throw new XMLException("Unknown mapping mode: '" + mappingMode.toString() + "'");
            }

            // Get target parent node. If target parent node doesn't exist then create it
            final String[] targetPath = targetXPath.split(XPATH_DELIMITER);
            final StringBuilder tmpPath = new StringBuilder();
            for (int i = 0; i < targetPath.length - 1; i++) {
                if (targetPath[i].length() > 0) {
                    tmpPath.append(XPATH_DELIMITER).append(targetPath[i]);
                }
            }

            final String targetParentPath = tmpPath.toString();

            String targetNodeName;
            Node targetParentNode;
            if (targetParentPath.length() == 0) {
                // The target node is the document root node
                targetParentNode = xmlSupport.createElementTree(targetDoc, targetXPath);
                targetNodeName = "";
            } else {
                // The target node is a child node
                targetParentNode = xmlSupport.createElementTree(targetDoc, targetParentPath);
                targetNodeName = targetPath[targetPath.length - 1];
            }

            // Loop over all source nodes and import them into the target doc
            for (int sourceIndex = 0; sourceIndex < sourceNodes.getLength(); sourceIndex++) {
                Node sourceNode = sourceNodes.item(sourceIndex);
                Element sourceElement = null;
                if (sourceNode.getNodeType() == Node.ELEMENT_NODE) {
                    sourceElement = (Element) sourceNode;
                } else if (sourceNode.getNodeType() == Node.DOCUMENT_NODE) {
                    sourceElement = sourceDoc.getDocumentElement();
                }
                if (sourceElement == null) {
                    //Never expected to happen
                    throw new XMLException("Source node has an unsupported type.");
                }
                final Element importElement = (Element) targetDoc.importNode(sourceElement, /* deep */true);

                Node targetElement;
                if (targetNodeName.length() == 0) {
                    targetElement = targetParentNode;
                } else {
                    targetElement = xmlSupport.createElement(targetDoc, targetNodeName);
                    targetParentNode.appendChild(targetElement);
                }
                if (targetElement == null) {
                    // This happens if the targetXPath is "/", so no root node exists. In that case, create a root node with the
                    //name of the source element
                    targetElement = xmlSupport.createElement(targetDoc, sourceElement.getNodeName());
                    targetDoc.appendChild(targetElement);
                }

                // Copy the attributes of the source element to the target element
                final NamedNodeMap attrs = importElement.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    final Attr importAttr = (Attr) targetDoc.importNode(attrs.item(i), true);
                    targetElement.getAttributes().setNamedItem(importAttr);
                }

                // Move all the children
                while (importElement.hasChildNodes()) {
                    targetElement.appendChild(importElement.getFirstChild());
                }
            }
        }
    }

    /**
     * Does the mapping between the elements of a source document and a target document.
     * 
     * @param sourceFile The name of the source document whose elements should be mapped.
     * @param targetFile The name of the target document.
     * @param mappingsFile A document with a list of mapping rules.
     * @throws XPathExpressionException Thrown if XPath could not be evaluated.
     * @throws XMLException Mapping error.
     * 
     */
    @Override
    public void transformXMLFileWithXMLMappingInformation(File sourceFile, File targetFile, File mappingsFile)
        throws XPathExpressionException, XMLException {
        Document mappingsDoc = xmlSupport.readXMLFromFile(mappingsFile);
        transformXMLFileWithXMLMappingInformation(sourceFile, targetFile, mappingsDoc);
    }

    @Override
    public void transformXMLFileWithXMLMappingInformation(File sourceFile, File targetFile, Document mappingsDoc)
        throws XPathExpressionException, XMLException {
        synchronized (XMLMapperConstants.GLOBAL_MAPPING_LOCK) {
            Document sourceDoc = xmlSupport.readXMLFromFile(sourceFile);
            Document targetDoc;
            if (targetFile.exists()) {
                targetDoc = xmlSupport.readXMLFromFile(targetFile);
            } else {
                targetDoc = xmlSupport.createDocument();
            }
            transformXMLFileWithXMLMappingInformation(sourceDoc, targetDoc, mappingsDoc);
            xmlSupport.writeXMLtoFile(targetDoc, targetFile);
        }
    }

    /**
     * Reads the mapping information from a mapping file and builds a list of mapping rules. Visibility is protected instead of private to
     * make it testable.
     * 
     * @param mappingDoc The mapping file as DOM document.
     * @return Returns a list of XMLMappingInformation objects, which contain the mapping rules.
     * @throws XMLException Mapping error.
     * 
     */
    protected List<XMLMappingInformation> readXMLMapping(Document mappingsDoc) throws XMLException {
        final List<XMLMappingInformation> mappings = new LinkedList<XMLMappingInformation>();

        try {
            final XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new XMLNamespaceContext(mappingsDoc));
            final NodeList nodeList = (NodeList) xpath.evaluate("/map:mappings/map:mapping", mappingsDoc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                final XMLMappingInformation mapInfo = new XMLMappingInformation();
                final Element current = (Element) nodeList.item(i);

                // Read in the attributes of the current map:mapping element
                final NamedNodeMap attrs = current.getAttributes();
                for (int j = 0; j < attrs.getLength(); j++) {
                    final Attr mapAttr = (Attr) attrs.item(j);
                    if (mapAttr.getName().equals("mode")) {
                        if (mapAttr.getValue().equals("delete")) {
                            mapInfo.setMode(EMappingMode.Delete);
                        } else if (mapAttr.getValue().equals("delete-only")) {
                            mapInfo.setMode(EMappingMode.DeleteOnly);
                        } else if (mapAttr.getValue().equals("append")) {
                            mapInfo.setMode(EMappingMode.Append);
                        } else {
                            throw new XMLException("Unknown mapping mode: '" + mapAttr.getValue() + "'");
                        }
                    }
                }

                // <source> element
                final Node source = (Node) xpath.evaluate("map:source", current, XPathConstants.NODE);
                if (source != null) {
                    mapInfo.setSourceXPath(source.getTextContent().trim());
                    if (mapInfo.getSourceXPath().length() == 0) {
                        throw new XMLException("Empty <map:source> element found in mapping file");
                    }
                } else if (mapInfo.getMode() != EMappingMode.DeleteOnly) {
                    throw new XMLException("No <map:source> element found in mapping file");
                }

                // <target> element
                final Node target = (Node) xpath.evaluate("map:target", current, XPathConstants.NODE);
                if (target != null) {
                    mapInfo.setTargetXPath(target.getTextContent().trim());
                    if (mapInfo.getTargetXPath().length() == 0) {
                        throw new XMLException("Empty <map:target> element found in mapping file");
                    }
                } else {
                    throw new XMLException("No <map:target> element found in mapping file");
                }

                mappings.add(mapInfo);
            }
        } catch (final XPathExpressionException e) {
            throw new XMLException(
                "XML mapping error. No mapping nodes (/map:mappings/map:mapping) found in the mapping file."
                    + " Please ensure that your mapping file contains the corresponding nodes and uses the corresponding namespace"
                    + " (xmlns:map=\"https://rcenvironment.de/2015/mapping\")", e);
        }
        return mappings;
    }

    /**
     * Creates a mapping document for the 'append' mapping mode. This mapping document contains mapping rules for every leaf element of a
     * given source node. (adapted from old XML mapper)
     * 
     * @param sourceElement The source element for whose leafs the mapping rules must be created.
     * @param mapInfo Current mapping information with source and target XPaths
     * @return Returns a new mapping document with mapping rules for all leafs of the current element.
     */
    private Document createXPathMappings(final Element sourceElement, final XMLMappingInformation mapInfo) throws XMLException {
        try {
            final String sourceString = xmlSupport.writeXMLToString(sourceElement);
            final Source source = new StreamSource(new StringReader(sourceString));

            final Document mappingDoc = xmlSupport.createDocument();
            final DOMResult result = new DOMResult(mappingDoc);

            // Create new transformer with parameters.
            StreamSource inStreamXPath = new StreamSource(getClass().getClassLoader().getResourceAsStream("CreateXPaths.xslt"));
            final Transformer transformer = tFactory.newTransformer(inStreamXPath);
            transformer.setErrorListener(new XSLTErrorHandler());
            transformer.setParameter("sourceXPath", mapInfo.getSourceXPath());
            transformer.setParameter("targetXPath", mapInfo.getTargetXPath());

            transformer.transform(source, result);
            return mappingDoc;
        } catch (TransformerException e) {
            throw new XMLException("Failed to create XPath mapping", e);
        }
    }
}

/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.utils.common.xml;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.utils.incubator.XMLHelper;
import de.rcenvironment.core.utils.incubator.XMLNamespaceContext;
import de.rcenvironment.core.utils.incubator.XSLTErrorHandler;
import de.rcenvironment.cpacs.utils.common.xml.internal.EMappingMode;

/**
 * This class does the mapping between two XML files. Additionally it has some utility methods, i.e. for building a list of mapping rules.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 * @author Arne Bachmann
 */
public class XMLMapper {

    /**
     * Our logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(XMLMapper.class);

    /**
     * The bundle to read the xslt filename from (added to both client and server by the build scripts).
     */
    private static final ResourceBundle COMMON = ResourceBundle.getBundle("Common");

    /**
     * Helper for reading/writing/changing XML documents.
     */
    private static XMLHelper xmlHelper = new XMLHelper();

    /**
     * Reads the mapping information from a mapping file and builds a list of mapping rules.
     * 
     * @param mappingDoc The mapping file as DOM document.
     * @return Returns a list of XMLMappingInformation objects, which contain the mapping rules.
     * @throws ComponentException Mapping error.
     */
    public List<XMLMappingInformation> readXMLMapping(final Document mappingDoc) throws ComponentException {
        final List<XMLMappingInformation> mappings = new LinkedList<XMLMappingInformation>();

        try {
            final XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new XMLNamespaceContext(mappingDoc));
            final NodeList nodeList = (NodeList) xpath.evaluate("/map:mappings/map:mapping", mappingDoc, XPathConstants.NODESET);
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
                        } else if (mapAttr.getValue().equals("append")) {
                            mapInfo.setMode(EMappingMode.Append);
                        } else {
                            LOGGER.error("Unknown mapping mode: '" + mapAttr.getValue() + "'");
                        }
                    }
                }

                // <source> element
                final Node source = (Node) xpath.evaluate("map:source", current, XPathConstants.NODE);
                if (source != null) {
                    mapInfo.setSourceXPath(source.getTextContent().trim());
                    if (mapInfo.getSourceXPath().length() == 0) {
                        LOGGER.error("Empty <map:source> element found in mapping file, skipping entry...");
                        continue;
                    }
                } else {
                    LOGGER.error("No <map:source> element found in mapping file, skipping entry...");
                    continue;
                }

                // <target> element
                final Node target = (Node) xpath.evaluate("map:target", current, XPathConstants.NODE);
                if (target != null) {
                    mapInfo.setTargetXPath(target.getTextContent().trim());
                    if (mapInfo.getTargetXPath().length() == 0) {
                        LOGGER.error("Empty <map:target> element found in mapping file, skipping entry...");
                        continue;
                    }
                } else {
                    LOGGER.error("No <map:target> element found in mapping file, skipping entry...");
                    continue;
                }

                // Add mapping to the other mappings
                /*
                 * logger.debug("Adding mapping entry map:source='" + mapInfo.getSourceXPath() + "', map:target='" +
                 * mapInfo.getTargetXPath() + "', mapping mode = " + mapInfo.getMode().toString());
                 */
                mappings.add(mapInfo);
            }
        } catch (final XPathExpressionException e) {
            throw new ComponentException(
                "XML mapping error. No mapping nodes (/map:mappings/map:mapping) found in the mapping file."
                    + " Please ensure that your mapping file contains the corresponding nodes and uses the corresponding namespace"
                    + " (xmlns:map=\"http://www.rcenvironment.de/2015/mapping\").",
                e.getCause());
        }
        return mappings;
    }

    /**
     * Does the mapping between the elements of a source document and a target document.
     * 
     * @param sourceDoc The source document whose elements should be mapped.
     * @param targetDoc The target document.
     * @param mappings A list of mapping rules.
     * @throws XPathExpressionException Thrown if XPath could not be evaluated.
     * @throws ComponentException Mapping error.
     */
    public void map(final Document sourceDoc, final Document targetDoc, final List<XMLMappingInformation> mappings)
        throws XPathExpressionException, ComponentException {
        // XPath object for querying the source document
        final XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new XMLNamespaceContext(sourceDoc));

        // Loop over all mapping rules
        for (final Iterator<XMLMappingInformation> it = mappings.iterator(); it.hasNext();) {
            final XMLMappingInformation mapInfo = it.next();
            final EMappingMode mappingMode = mapInfo.getMode();
            final String sourceXPath = mapInfo.getSourceXPath();
            final String targetXPath = mapInfo.getTargetXPath();

            /*
             * logger.debug(" Processing mapping entry map:source='" + sourceXPath + "', map:target='" + targetXPath + "', mapping mode = "
             * + mappingMode.toString().toLowerCase());
             */
            final NodeList sourceNodes = (NodeList) xpath.evaluate(sourceXPath, sourceDoc, XPathConstants.NODESET);
            if (sourceNodes.getLength() == 0) {
                LOGGER.warn(" No source elements found for map:source='" + sourceXPath + "', skipping mapping rule");
                // continue;
            }

            switch (mappingMode) {
            case Append:
                LOGGER.debug(" Creating and processing temporary mapping entries for append mode...");
                final Document mappingDoc = createXPathMappings(sourceNodes.item(0), mapInfo);
                final List<XMLMappingInformation> appendMappings = readXMLMapping(mappingDoc);
                map(sourceDoc, targetDoc, appendMappings);
                LOGGER.debug(" Finished creating and processing temporary mapping entries for append mode");
                continue;
            case Delete:
                LOGGER.debug(" Deleting target element");
                xmlHelper.deleteElement(targetDoc, mapInfo.getTargetXPath());
                break;
            default:
                LOGGER.error(" Unknown mapping mode, skipping mapping rule.");
            }

            // Get target parent node. If target parent node doesn't exist then create it
            final String[] targetPath = targetXPath.split(XMLHelper.XPATH_DELIMITER);
            final StringBuilder tmpPath = new StringBuilder();
            for (int i = 0; i < targetPath.length - 1; i++) {
                if (targetPath[i].length() > 0) {
                    tmpPath.append(XMLHelper.XPATH_DELIMITER).append(targetPath[i]);
                }
            }

            final String targetParentPath = tmpPath.toString();

            String targetNodeName;
            Node targetParentNode;
            if (targetParentPath.length() == 0) {
                // The target node is the document root node
                targetParentNode = xmlHelper.createElementTree(targetDoc, mapInfo.getTargetXPath());
                targetNodeName = "";
            } else {
                // The target node is a child node
                targetParentNode = xmlHelper.createElementTree(targetDoc, targetParentPath);
                targetNodeName = targetPath[targetPath.length - 1];
            }

            // Loop over all source nodes and import them into the target doc
            for (int sourceIndex = 0; sourceIndex < sourceNodes.getLength(); sourceIndex++) {
                final Element sourceElement = (Element) sourceNodes.item(sourceIndex);
                final Element importElement = (Element) targetDoc.importNode(sourceElement, /* deep */true);

                Node targetElement;
                if (targetNodeName.length() == 0) {
                    targetElement = targetParentNode;
                } else {
                    targetElement = xmlHelper.createElement(targetDoc, targetNodeName);
                    targetParentNode.appendChild(targetElement);
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
     * Creates a mapping document for the 'append' mapping mode. This mapping document contains mapping rules for every leaf element of a
     * given source node.
     * 
     * @param sourceNode The source element for whose leafs the mapping rules must be created.
     * @param mapInfo Current mapping information with source and target XPaths
     * @return Returns a new mapping document with mapping rules for all leafs of the current element.
     */
    private Document createXPathMappings(final Node sourceNode, final XMLMappingInformation mapInfo) {
        try {
            XSLTErrorHandler errorHandler = new XSLTErrorHandler();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setErrorListener(errorHandler);

            // Reads the XSLT stylesheet from the jar file or from the package path.
            final InputStream inStream = this.getClass().getResourceAsStream(COMMON.getString("formatter.xslt"));
            Transformer formatter = transformerFactory.newTransformer(new StreamSource(inStream));
            formatter.setErrorListener(new XSLTErrorHandler());

            final String sourceString = xmlHelper.writeXML((Element) sourceNode);
            final Source source = new StreamSource(new StringReader(sourceString));

            final Document mappingDoc = xmlHelper.createDocument();
            final DOMResult result = new DOMResult(mappingDoc);

            final InputStream inStreamXPath = this.getClass().getResourceAsStream(COMMON.getString("xpaths.xslt"));
            final Transformer transformer = transformerFactory.newTransformer(new StreamSource(inStreamXPath));
            transformer.setErrorListener(new XSLTErrorHandler());
            transformer.setParameter("sourceXPath", mapInfo.getSourceXPath());
            transformer.setParameter("targetXPath", mapInfo.getTargetXPath());

            transformer.transform(source, result);
            return mappingDoc;
        } catch (final TransformerConfigurationException tce) {
            LOGGER.fatal(tce.getMessage(), tce);
        } catch (final TransformerException te) {
            LOGGER.fatal(te.getMessage(), te);
        }
        return null;
    }

}

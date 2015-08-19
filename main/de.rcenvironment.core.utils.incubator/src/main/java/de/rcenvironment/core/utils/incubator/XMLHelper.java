/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.rcenvironment.core.utils.incubator.xml.XMLException;

/**
 * 
 * Helper class for XML handling (parsing, DOM handling).
 * 
 * Note: This class has not a good quality. It should be really reworked. It should not be declared as deprecated because it is used at many
 * spots in RCE for CPACS distribution. -Markus Kunde
 * 
 * @author Markus Litz, Markus Kunde, Arne Bachmann
 */
public class XMLHelper {

    /**
     * XPath delimiter string (slash instead of backslash).
     */
    public static final String XPATH_DELIMITER = "/";

    /**
     * Our logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(XMLHelper.class);

    private static final String MSG_INVALID_X_PATH_EXPRESSION = "Invalid XPath expression";

    /**
     * This enumerator represents three choices regarding the manipulation of elements.
     */
    private enum EElementManipulation {

        /** OVERWRITE if an element already exists it will be overwritten. */
        OVERWRITE,

        /** ATTACH if an element already exists, it will be attached to the same level. */
        ATTACH,

        /** NOTHING if an element already exists, nothing happened. */
        NOTHING
    };

    /**
     * Three times used.
     */
    private static final String PARSER_CONFIGURATION_ERROR = "Parser configuration error:";

    /**
     * Three times used.
     */
    private static final String SAX_ERROR = "SAX error:";

    /**
     * Quote character string used in XPath attributes.
     */
    private static final String QUOTE_CHAR = "\"";

    /**
     * XML-Namespace delimiter character.
     */
    private static final String NS_DELIMITER = ":";

    /**
     * Singleton factory instance to be reused by all methods.
     */
    private TransformerFactory transformerFactory;

    /** This one formats XML (see XMLFormatter.xslt). */
    private Transformer formatter;

    /**
     * The factory used in some places.
     */
    private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    /**
     * Factory for xpath objects.
     */
    private XPathFactory xpathFactory;

    /**
     * One handler reused in the factory and all transformers.
     */
    private XSLTErrorHandler errorHandler;

    /**
     * This constructor initializes the formatter, used in many places.
     */
    public XMLHelper() {
        // Initialize the formatter transformer
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        try {
            errorHandler = new XSLTErrorHandler();
            transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setErrorListener(errorHandler);

            // Reads the XSLT stylesheet from the jar file or from the package path.
            final InputStream inStream = getClass().getResourceAsStream("/resources/XMLFormatter.xslt");

            formatter = transformerFactory.newTransformer(new StreamSource(inStream));
            formatter.setErrorListener(new XSLTErrorHandler());
        } catch (final TransformerConfigurationException tce) {
            LOGGER.fatal(tce.getMessage(), tce);
        }
        xpathFactory = createXPathFactory();
    }

    /**
     * Workaround for JVM bug.
     * 
     * @return A valid xpath factory
     */
    private static XPathFactory createXPathFactory() {
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0], oldClassLoader));
        final XPathFactory xpathFactory = XPathFactory.newInstance();
        Thread.currentThread().setContextClassLoader(oldClassLoader);
        return xpathFactory;
    }

    /**
     * Reads an XML file and returns the DOM tree of it.
     * 
     * @param filename File name of the XML file to read and parse.
     * @return Returns the DOM document of the XML file.
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    public Document readXMLFromFile(final String filename) throws XMLException {
        return readXMLFromFile(new File(filename));
    }

    /**
     * Reads an XML file and return the DOM tree of it.
     * 
     * @param file The file object to read from
     * @return The DOM document of the XML file.
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    public Document readXMLFromFile(final File file) throws XMLException {
        Document doc = null;
        String errorMessage = "Error read XML from file";
        try {
            final DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new XMLErrorHandler());
            doc = db.parse(file);
        } catch (final ParserConfigurationException e) {
            LOGGER.warn(PARSER_CONFIGURATION_ERROR, e);
            throw new XMLException(errorMessage, e);
        } catch (final SAXException e) {
            LOGGER.warn(SAX_ERROR, e);
            throw new XMLException(errorMessage, e);
        } catch (final IOException e) {
            LOGGER.debug("IO error: " + e.getMessage());
            throw new XMLException(errorMessage, e);
        }
        return doc;
    }

    /**
     * Reads an XML stream and return the DOM tree of it.
     * 
     * @param stream The input stream to read from
     * @return The DOM document of the XML file.
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    public Document readXMLFromStream(final InputStream stream) throws XMLException {
        Document doc = null;
        String errorMessage = "Error read XML from stream";
        try {
            final DocumentBuilder db = dbf.newDocumentBuilder();
            if (stream == null) {
                return db.newDocument();
            }
            db.setErrorHandler(new XMLErrorHandler());
            doc = db.parse(stream);
        } catch (final ParserConfigurationException e) {
            LOGGER.warn(PARSER_CONFIGURATION_ERROR, e);
            throw new XMLException(errorMessage, e);
        } catch (final SAXException e) {
            LOGGER.warn(SAX_ERROR, e);
            throw new XMLException(errorMessage, e);
        } catch (final IOException e) {
            LOGGER.debug("IO error: " + e.getMessage());
            throw new XMLException(errorMessage, e);
        }
        return doc;
    }

    /**
     * Parses XML stored in a String and returns the DOM tree of it.
     * 
     * @param aString The String containing the XML.
     * @return The DOM document or null if any exception was caught
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    public Document readXMLFromString(final String aString) throws XMLException {
        Document doc = null;
        String errorMessage = "Error read XML from string";
        try {
            final DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new XMLErrorHandler());
            doc = db.parse(new InputSource(new StringReader(aString)));
        } catch (final ParserConfigurationException e) {
            LOGGER.error(PARSER_CONFIGURATION_ERROR, e);
            throw new XMLException(errorMessage, e);
        } catch (final SAXException e) {
            LOGGER.error(SAX_ERROR, e);
            throw new XMLException(errorMessage, e);
        } catch (final IOException e) {
            LOGGER.error("IO error:", e);
            throw new XMLException(errorMessage, e);
        }
        return doc;
    }

    /**
     * Simple access to read XML from an URL.
     * 
     * @param urlString The url to read from
     * @return The document read in or an empty document if an error occurred
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    public Document readXMLFromUrl(final String urlString) throws XMLException {
        Document doc;
        BufferedReader stdin = null;
        try {
            final URL url = new URL(urlString);
            final URLConnection conn = url.openConnection();
            final InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            stdin = new BufferedReader(isr);
            String input = "";
            String thisLine = "";
            while ((thisLine = stdin.readLine()) != null) { // while loop begins here
                input += thisLine;
            }
            int index = input.indexOf("<?xml");
            if (index > 0) {
                input = input.substring(index); // remove BOM
            }
            doc = readXMLFromString(input);
        } catch (final IOException e) {
            LOGGER.error(e);
            doc = createDocument();
        } finally {
            try {
                if (stdin != null) { // implicitly closing the inputstreadreader
                    stdin.close();
                }
            } catch (final IOException e) {
                LOGGER.debug("Catched IOException");
            }
        }
        return doc;
    }

    /**
     * Creates a whole element tree described by a given XPath expression.
     * 
     * @param doc The document where to create the element tree.
     * @param xPathStr The XPath expression describing the element tree to be created.
     * @return Returns the last child node (leaf node) of the created element tree or null if PathStr is empty.
     */
    public Node createElementTree(final Document doc, final String xPathStr) {
        Node parentNode = doc.getDocumentElement();

        try {
            final XPath xpath = xpathFactory.newXPath();
            xpath.setNamespaceContext(new XMLNamespaceContext(doc));

            final StringBuilder currPath = new StringBuilder("");
            final String[] elements = xPathStr.split(XPATH_DELIMITER);

            for (String element : elements) {
                if (element.length() == 0) {
                    continue;
                }

                // Test if node exists for the current xpath
                currPath.append(XPATH_DELIMITER).append(element);
                final Node tempNode = (Node) xpath.evaluate(currPath.toString(), doc, XPathConstants.NODE);
                if (tempNode != null) {
                    parentNode = tempNode;
                    continue;
                }

                // Node doesn't exists, create it
                final Element elementNode = createElement(doc, element);

                if (parentNode == null) {
                    doc.appendChild(elementNode);
                } else {
                    parentNode.appendChild(elementNode);
                }

                parentNode = elementNode;
            }
        } catch (final XPathExpressionException e) {
            LOGGER.fatal(MSG_INVALID_X_PATH_EXPRESSION, e);
        }
        return parentNode;
    }

    /**
     * Creates an XML element from an element string.
     * 
     * @param doc The document for which the element should be created.
     * @param elementString The string describing the element. It must contain the element name and may contain one or more predicates in
     *        braces. If these predicates contain an attribute, the element is created with this attribute. If the predicate describes an
     *        subelement of the element, the subelement is created.
     * 
     *        Example: Tool[name="IBUCK"][last_change="07.10.2005 16:30:00"][toolOuput] will create <br>
     *        {@code &lt;Tool name="IBUCK" last_change="07.10.2005 16:30:00"><br>
     * &lt;toolOutput>&lt;/toolOutput><br>
     * &lt;/Tool> }
     * 
     * @return Returns the created element with all its attributes and subelements.
     */
    public Element createElement(final Document doc, final String elementString) {

        final String[] elementParts = elementString.split("\\[|\\]");
        if (elementParts.length == 0) {
            return null;
        }

        final String elementName = elementParts[0].trim();
        if (elementName.length() == 0) {
            return null;
        }

        // Test if element name contains a namespace prefix and create element
        Element elementNode;
        final String[] nameParts = elementName.split(NS_DELIMITER);
        if (nameParts.length == 1) {
            elementNode = doc.createElement(elementName);
        } else {
            elementNode = doc.createElementNS(doc.lookupNamespaceURI(nameParts[0]), elementName);
        }

        // Loop over element predicates of the form
        // [@attrname1="value1"] or
        // [@ns:attrname2="value2"] or
        // [ns:subnodename] or
        // [subnodename="value3"] etc.
        for (int j = 1; j < elementParts.length; j++) {
            final String predicate = elementParts[j].trim();
            if (predicate.length() == 0) {
                continue;
            }
            if (Character.isDigit(predicate.charAt(0))) {
                continue;
            }
            if (predicate.startsWith("@")) {
                // It's an attribute of the current element
                // Remove '@' at the beginning
                String attrStr = predicate.substring(1);
                String[] attribute = attrStr.split("=");
                String attrName = attribute[0];
                String attrValue = "";
                if (attribute.length == 2) {
                    attrValue = attribute[1];
                }
                if (attrValue.startsWith(QUOTE_CHAR)) {
                    attrValue = attrValue.substring(1);
                }
                if (attrValue.endsWith(QUOTE_CHAR)) {
                    attrValue = attrValue.substring(0, attrValue.length() - 1);
                }

                // Test if attribute name contains a namespace prefix
                String[] attrNameParts = attrName.split(NS_DELIMITER);
                if (attrNameParts.length == 1) {
                    elementNode.setAttribute(attrName, attrValue);
                } else {
                    elementNode.setAttributeNS(doc.lookupNamespaceURI(attrNameParts[0]), attrName, attrValue);
                }
            } else {
                // It's an subelement of the current element
                Element subNode;
                String[] subNodeParts = predicate.split("=");
                String subNodeName = subNodeParts[0];
                String subNodeValue = "";
                if (subNodeParts.length == 2) {
                    subNodeValue = subNodeParts[1];
                }
                if (subNodeValue.startsWith(QUOTE_CHAR)) {
                    subNodeValue = subNodeValue.substring(1);
                }
                if (subNodeValue.endsWith(QUOTE_CHAR)) {
                    subNodeValue = subNodeValue.substring(0, subNodeValue.length() - 1);
                }

                // Test if element name contains a namespace prefix
                String[] subNodeNameParts = predicate.split(NS_DELIMITER);
                if (subNodeNameParts.length == 1) {
                    subNode = doc.createElement(subNodeName);
                } else {
                    subNode = doc.createElementNS(doc.lookupNamespaceURI(subNodeNameParts[0]), subNodeName);
                }

                if (subNodeValue.length() > 0) {
                    // The subelement contains a text value
                    Text textNode = doc.createTextNode(subNodeValue);
                    subNode.appendChild(textNode);
                }

                // Finally append the subnode to the current element
                elementNode.appendChild(subNode);
            }
        }
        return elementNode;
    }

    /**
     * Creates an empty DOM document.
     * 
     * @return Returns the new document.
     */
    public Document createDocument() {
        Document tempDoc = null;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            tempDoc = db.newDocument();
        } catch (final ParserConfigurationException e) {
            LOGGER.fatal("Parser configuration error", e);
        }
        return tempDoc;
    }

    /**
     * Deletes an element and multiple occurences of an element.
     * 
     * @param doc The DOM document which contains the element.
     * @param xPathStr The XPath expression describing the path of the element.
     */
    public void deleteElement(final Document doc, final String xPathStr) {
        try {
            XPath xpath = xpathFactory.newXPath();
            xpath.setNamespaceContext(new XMLNamespaceContext(doc));

            final NodeList nodes = (NodeList) xpath.evaluate(xPathStr, doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node node = nodes.item(i);
                final Node parentNode = node.getParentNode();
                parentNode.removeChild(node);
            }
        } catch (final XPathExpressionException e) {
            LOGGER.fatal(MSG_INVALID_X_PATH_EXPRESSION, e);
        }
    }

    /**
     * Replaces the text element of a node.
     * 
     * @param doc The DOM document which contains the element.
     * @param xPathStr The XPath expression describing the path of the element.
     * @param newValue The new value that should replace the old value at the xpath position.
     * @throws IllegalArgumentException if xpath refers to no node
     * @return True if success
     */
    public boolean replaceNodeText(final Document doc, final String xPathStr, final String newValue) throws IllegalArgumentException {
        try {
            XPath xpath = xpathFactory.newXPath();
            xpath.setNamespaceContext(new XMLNamespaceContext(doc));

            final NodeList nodes = (NodeList) xpath.evaluate(xPathStr, doc, XPathConstants.NODESET);
            if (nodes.getLength() == 0) {
                // Throw exception if element does not exist
                throw new IllegalArgumentException("XPath refers to no node.");
            } else {
                for (int i = 0; i < nodes.getLength(); i++) {
                    final Node node = nodes.item(i);
                    node.setTextContent(newValue);
                }
            }

            return true;
        } catch (final XPathExpressionException e) {
            LOGGER.fatal(MSG_INVALID_X_PATH_EXPRESSION, e);
            return false;
        }
    }

    /**
     * Outputs a DOM document as pretty print XML file, returning null. If filename is null, the method returns the contents directly,
     * instead of writing to a file.
     * 
     * @param doc The DOM document to be printed.
     * @param filename File name of the output XML file to be created.
     * @return null if filename given, the file's results if not.
     */
    public String writeXML(final Document doc, final String filename) {
        // The next statement is needed to work around a bug in the JDK 1.4
        // concerning the escaping of space characters in file names.
        final Writer resultW = new StringWriter();
        StreamResult result;
        if (filename == null) {
            result = new StreamResult(resultW); // write to temp. string
        } else {
            final String fileURI = new File(filename).toURI().getPath();
            result = new StreamResult(fileURI); // write to temp. file
        }
        format(doc, result);
        if (filename == null) {
            return result.getWriter().toString();
        }
        return null;
    }

    /**
     * Wrapper for writing to a string instead of a file.
     * 
     * @param doc the document to write
     * @return the string containing the complete XML document
     */
    public String writeXML(final Document doc) {
        return writeXML(doc, null);
    }

    /**
     * Outputs a DOM element and all of its subelements as pretty print XML into a string.
     * 
     * @param sourceElement The DOM element to be print.
     * @return Returns the XML string of an element and its subelements.
     */
    public String writeXMLToString(final Element sourceElement) {
        String xmlString = "";

        Document tempDoc = createDocument();
        Element importElement = (Element) tempDoc.importNode(sourceElement, true);
        tempDoc.appendChild(importElement);
        xmlString = writeXMLToString(tempDoc);

        return xmlString;
    }

    /**
     * Outputs a DOM document as pretty print XML into a String.
     * 
     * @param doc The DOM document to be printed.
     * @return A string containing the XML.
     */
    public String writeXMLToString(final Document doc) {
        final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        format(doc, result);
        return writer.toString();
    }

    /**
     * 
     * Outputs a DOM element and all of its subelements as pretty print XML into a string.
     * 
     * @param sourceElement The DOM element to be print.
     * @return Returns the XML string of an element and its subelements.
     */
    public String writeXML(Element sourceElement) {
        String xmlString = "";

        Document tempDoc = createDocument();
        Element importElement = (Element) tempDoc.importNode(sourceElement, true);
        tempDoc.appendChild(importElement);
        xmlString = writeXML(tempDoc);

        return xmlString;
    }

    /**
     * Helper method to transform via the formatter xslt.
     * 
     * @param doc the document tree to format
     * @param result the result stream
     */
    public void format(final Document doc, final StreamResult result) {
        try {
            final DOMSource source = new DOMSource(doc);
            formatter.transform(source, result);
        } catch (final TransformerException te) {
            LOGGER.fatal(te.getMessage(), te);
        }
    }

    /**
     * 
     * Checks if a XML elements has children XML elements.
     * 
     * @param element The element to check.
     * @return True if there are subelements.
     */
    public boolean hasSubelements(final Element element) {
        boolean hasSubelements = false;

        final NodeList childs = element.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            final Node child = childs.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasSubelements = true;
                break;
            }
        }
        return hasSubelements;
    }

    /**
     * Returns the values of all subnodes of a given node type of an element as string. TODO this is programmed to work for more than one
     * value, but this is not desirable (???) since they would be concatenated without separator. Suggestion: Use String instead of Buffer
     * again and put a break into the loop, or use a (comma) separator
     * 
     * @param element The DOM element.
     * @param nodeType The searched node type.
     * @return The values of all subnodes as string.
     */
    public String getElementValues(final Element element, final short nodeType) {
        final StringBuffer data = new StringBuffer();
        final NodeList childs = element.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            final Node child = childs.item(i);
            if (child.getNodeType() == nodeType) {
                data.append(child.getNodeValue());
            }
        }
        return data.toString();
    }

    /**
     * Finds direct subnodes by their tag name.
     * 
     * @param doc A document node
     * @param name the name to find
     * @return the node or null
     */
    public Element getElementByName(final Node doc, final String name) {
        final NodeList nodes = doc.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName().equals(name)) {
                return (Element) nodes.item(i);
            }
        }
        return null;
    }

    /**
     * Return an attribute's value by a (relative) xpath.
     * 
     * @param node The relative root node to the parent element
     * @param name The name of the attribute
     * @return The attribute value or null
     */
    public String getAttributeValue(final Node node, final String name) {
        final NamedNodeMap attrs = node.getAttributes();
        final Node nameNode = attrs.getNamedItem(name);
        if (nameNode == null) {
            return null;
        }
        return nameNode.getNodeValue();
    }

    /**
     * This method returns the element-text of the element mentioned by the parameter.
     * 
     * @param doc THe ducument of which the element should be read.
     * @param xpathStatement The specific element (if not known use method "String[] getXPathOfMatchingElements(String nodename)" for
     *        inspection) -> can be more than one element
     * @param hasChildren If the node is not the lowest level = true
     * @return Returns the element-text of the element mentioned by the parameter. In case of several matching elements, the returning array
     *         consists of more than one element
     */
    public String getElementText(final Document doc, final String xpathStatement, final boolean hasChildren) {
        String result = "";

        try {
            try {
                final XPath xpath = xpathFactory.newXPath();
                final Node node = (Node) xpath.evaluate(xpathStatement, doc, XPathConstants.NODE);
                if (node == null) {
                    result = null;
                } else {
                    if (!hasChildren) {
                        if (node.getFirstChild() == null) {
                            return null;
                        }
                        result = node.getFirstChild().getNodeValue();
                    } else {
                        final Document d = new XMLHelper().createDocument();
                        final Node elementNode = new XMLHelper().createElementTree(d, xpathStatement);
                        try {
                            elementNode.getParentNode().replaceChild(d.importNode(node, true), elementNode);
                        } catch (final RuntimeException e) {
                            LOGGER.error(e);
                        }
                        result = new XMLHelper().writeXMLToString(d.getDocumentElement());
                    }
                }
            } catch (final XPathExpressionException e) {
                result = "";
            }
        } catch (final DOMException e) {
            result = "";
        }

        return result;
    }

    /**
     * Inserting a node (e.g. with Childnodes) on a specific position.
     * 
     * @param doc The document to replace the node.
     * @param xpathStatement the xpath statement with position of this node.
     * @param n the node that needs to be inserted.
     */
    public void insertNodeOnPosition(Document doc, String xpathStatement, Node n) {
        try {
            final XPath xpath = xpathFactory.newXPath();
            final NodeList nodeList = (NodeList) xpath.evaluate(xpathStatement, doc, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node item = nodeList.item(i);
                item.getParentNode().replaceChild(doc.importNode(n, true), item);
            }
        } catch (final XPathExpressionException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Creates an element at a specific position. TODO If text is filled: Working; in case text = empty: rewrite -> eg multiple elements
     * available and doing overwrite, etc.
     * 
     * @param doc The Document in which the element should be created.
     * @param xpathStatement The specific path (parentnode) of the element. If not unique, multiple elements will be manipulated regarding
     *        parameter em
     * @param element The element's name. If empty "''" a subelement will be created
     * @param text The value of the element
     * @param em in case the specific element already exists. (Only element is proven; value is not considered) OVERWRITE overwrite the
     *        element-text ATTACH attach the element on path NOTHING do nothing
     */
    public void createElementOnPosition(Document doc, String xpathStatement, String element, String text, EElementManipulation em) {
        try {
            final XPath xpath = xpathFactory.newXPath();

            if (text == null) {
                return;
            }

            final Node newChild = doc.createElement(element);
            if (!text.equalsIgnoreCase("")) {
                newChild.appendChild(doc.createTextNode(text));
            }

            NodeList nodeList = (NodeList) xpath.evaluate(xpathStatement, doc, XPathConstants.NODESET);

            if (nodeList.getLength() == 0) {
                // Create Hierarchy if needed
                String h = xpathStatement.replaceFirst("[" + XMLHelper.XPATH_DELIMITER + "]+", "");
                String[] e = h.split("[" + XMLHelper.XPATH_DELIMITER + "]+");
                String path = XMLHelper.XPATH_DELIMITER;
                for (String element2 : e) {
                    createElementOnPosition(doc, path, element2, "", EElementManipulation.NOTHING);
                    path = path + XMLHelper.XPATH_DELIMITER + element2;

                }
            }
            switch (em) {
            case OVERWRITE:
                boolean notExistent = false;
                nodeList = (NodeList) xpath.evaluate(xpathStatement + XMLHelper.XPATH_DELIMITER + element, doc, XPathConstants.NODESET);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    nodeList.item(i).getFirstChild().setNodeValue(text);
                    notExistent = true;
                }
                if (!notExistent) {
                    nodeList = (NodeList) xpath.evaluate(xpathStatement, doc, XPathConstants.NODESET);
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        nodeList.item(i).appendChild(newChild);
                    }
                }
                break;

            case ATTACH:
                for (int i = 0; i < nodeList.getLength(); i++) {
                    nodeList.item(i).appendChild(newChild);
                }
                break;

            case NOTHING:
                nodeList = (NodeList) xpath.evaluate(xpathStatement + XMLHelper.XPATH_DELIMITER + element, doc, XPathConstants.NODESET);
                if (nodeList.getLength() == 0) {
                    Node node = (Node) xpath.evaluate(xpathStatement, doc, XPathConstants.NODE);
                    node.appendChild(newChild);
                }
                break;

            default:
                break;
            }

        } catch (final XPathExpressionException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Evaluates an XPath statement on the given Document and returns the resulting nodelist.
     * 
     * @param doc The document on which the XPath is evaluated.
     * @param xpathStatement The XPath to evaluate.
     * @return The result NodeList
     */
    public NodeList getNodeListByXPath(final Node doc, final String xpathStatement) {
        NodeList nodeList = null;
        try {
            final XPath xpath = xpathFactory.newXPath();
            nodeList = (NodeList) xpath.evaluate(xpathStatement, doc, XPathConstants.NODESET);
        } catch (final XPathExpressionException e) {
            LOGGER.error(e);
        }
        return nodeList;
    }

    /**
     * Transforming an XML Source file with an xslt file to a target file.
     * 
     * @param sourceXML The xml source file.
     * @param xslTransformation The xslt file.
     * @param baseURI Bla
     * @return The document containing the result of the transforming. Or null if an error occurs during transformation.
     */
    public Document transform(final Document sourceXML, final Document xslTransformation, final String baseURI) {
        final Source xmlSourceDoc = new DOMSource(sourceXML);
        final Source xsltDoc = new DOMSource(xslTransformation);

        Document myToolDoc = null;
        try {
            DOMResult res = new DOMResult();
            final Transformer trans = transformerFactory.newTransformer(xsltDoc);

            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(OutputKeys.METHOD, "xml");

            trans.setURIResolver(new URIResolver() {
                @Override
                public Source resolve(String href, String base) {
                    return new StreamSource(new File(baseURI + File.separator + href));
                }
            });

            trans.transform(xmlSourceDoc, res);

            myToolDoc = (Document) res.getNode();
        } catch (final TransformerConfigurationException e) {
            LOGGER.error(e);
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
        } catch (final TransformerException e) {
            LOGGER.error(e);
        }

        return myToolDoc;
    }

}

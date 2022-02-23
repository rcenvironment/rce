/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.xml.api;

import java.io.File;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.rcenvironment.core.utils.common.xml.XMLException;


/**
 * Provides support for reading and writing XML files and accessing XML elements.
 * (Former: XMLHelper)
 * 
 * @author Brigitte Boden
 */
public interface XMLSupportService {

    /**
     * Reads an XML file and return the DOM tree of it.
     * 
     * @param file The file object to read from
     * @return The DOM document of the XML file.
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    Document readXMLFromFile(File file) throws XMLException;
    
    /**
     * Reads an XML stream and return the DOM tree of it.
     * 
     * @param stream The input stream to read from
     * @return The DOM document of the XML file.
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    Document readXMLFromStream(InputStream stream) throws XMLException;
    
    /**
     * Parses XML stored in a String and returns the DOM tree of it.
     * 
     * @param aString The String containing the XML.
     * @return The DOM document or null if any exception was caught
     * @throws XMLException Thrown if an error in handling the XML file occured.
     */
    Document readXMLFromString(String aString) throws XMLException;
    
    /**
     * Creates a whole element tree described by a given XPath expression.
     * 
     * @param doc The document where to create the element tree.
     * @param xPathStr The XPath expression describing the element tree to be created.
     * @return Returns the last child node (leaf node) of the created element tree or null if PathStr is empty.
     * @throws XMLException 
     */
    Node createElementTree(Document doc, String xPathStr) throws XMLException;
    
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
     * &lt;/Tool> }W
     * @return Returns the created element with all its attributes and subelements.
     * @throws XMLException 
     */
    Element createElement(Document doc, String elementString) throws XMLException;
    
    /**
     * Creates an empty DOM document.
     * 
     * @return Returns the new document.
     * @throws XMLException 
     */
    Document createDocument() throws XMLException; 
    
    /**
     * Deletes an element and multiple occurences of an element.
     * 
     * @param doc The DOM document which contains the element.
     * @param xPathStr The XPath expression describing the path of the element.
     * @throws XMLException 
     */
    void deleteElement(Document doc, String xPathStr) throws XMLException;
    
    /**
     * Replaces the text element of a node.
     * 
     * @param doc The DOM document which contains the element.
     * @param xPathStr The XPath expression describing the path of the element.
     * @param newValue The new value that should replace the old value at the xpath position.
     * @param generateIfNotExist True if a non-existing element should be generated.
     * @throws XMLException The XMLException thrown if an error occurs.
     */
    void replaceNodeText(Document doc, String xPathStr, String newValue, Boolean generateIfNotExist) throws XMLException;
    
    /**
     * Outputs a DOM document as an XML file.
     * 
     * @param doc The DOM document to be printed.
     * @param file Output XML file to be created.
     * @throws XMLException 
     */
    void writeXMLtoFile(Document doc, File file) throws XMLException;
    
    /**
     * Outputs a DOM document as XML into a String.
     * 
     * @param doc The DOM document to be printed.
     * @return The return string
     * @throws XMLException 
     */
    String writeXMLToString(Document doc) throws XMLException;
    
    /**
     * Outputs a DOM element and all of its subelements as pretty print XML into a string.
     * 
     * @param sourceElement The DOM element to be print.
     * @return Returns the XML string of an element and its subelements.
     * @throws XMLException 
     */
    String writeXMLToString(Element sourceElement) throws XMLException;
    
    /**
     * This method returns the element-text of the element mentioned by the parameter.
     * 
     * @param doc The document containing the element to be read.
     * @param xpathStatement The specific element. This method assumes the element does not have children!
     * @return Returns the element-text of the element mentioned by the parameter. 
     * @throws XMLException 
     */
    String getElementText(Document doc, String xpathStatement) throws XMLException;
    
}

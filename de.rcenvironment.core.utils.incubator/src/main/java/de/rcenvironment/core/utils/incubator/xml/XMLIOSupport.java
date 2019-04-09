/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

/**
 * Utility class that provides methods to read/write XML-documents from/to different
 * sources/targets. The underlying library is dom4j so when reading/writing XML-documents, an
 * <code>org.dom4j.Document</code> will be used.
 * 
 * @author Andre Nurzenski
 */
public final class XMLIOSupport {

    /**
     * 
     * Private constructor, because this is a utility class.
     * 
     */
    private XMLIOSupport() {

    }

    /**
     * 
     * Reads XML data from an input stream and returns a dom4j <code>Document</code>.
     * 
     * @param xmlInputStream
     *            An input stream of the XML data which is intend to be parsed.
     * @return a <code>Document</code> representation of the parsed file.
     * @throws DocumentException
     *             if an error occurs during parsing.
     */
    public static Document readXML(InputStream xmlInputStream) throws DocumentException {
        // Initialize the SAX reader and set several properties.
        SAXReader saxReader = new SAXReader();
        saxReader.setMergeAdjacentText(true);
        saxReader.setStringInternEnabled(true);
        saxReader.setStripWhitespaceText(true);

        // Read the XML file from the specified input stream.
        Document document = saxReader.read(xmlInputStream);

        return document;
    }

    /**
     * 
     * Reads an XML file and returns a dom4j <code>Document</code>.
     * 
     * @param filename
     *            The filename of the XML file which is intend to be parsed.
     * @return a <code>Document</code> representation of the parsed file.
     * @throws DocumentException
     *             if an error occurs during parsing.
     */
    public static Document readXML(String filename) throws DocumentException {
        return readXML(new File(filename));
    }

    /**
     * 
     * Reads an XML file and returns a dom4j <code>Document</code>.
     * 
     * @param file
     *            The XML file which is intend to be parsed.
     * @return a <code>Document</code> representation of the parsed file.
     * @throws DocumentException
     *             if an error occurs during parsing.
     */
    public static Document readXML(File file) throws DocumentException {
        // Initialize the SAX reader and set several properties.
        SAXReader saxReader = new SAXReader();
        saxReader.setMergeAdjacentText(true);
        saxReader.setStringInternEnabled(true);
        saxReader.setStripWhitespaceText(true);

        // Read the XML file from the specified file.
        Document document = saxReader.read(file);

        return document;
    }

    /**
     * 
     * Reads an XML file, validates it against a schema and returns a dom4j <code>Document</code>.
     * 
     * @param schema
     *            The schema file which is intend to be used for validation.
     * @param xml
     *            The XML file which is intend to be parsed.
     * @return a <code>Document</code> representation of the parsed file.
     * @throws SAXException
     *             if the specified XML file is not valid.
     * @throws IOException
     *             if an I/O exception occurs during validation.
     * @throws DocumentException
     *             if parsing the XML document failed.
     */
    public static Document readXML(String schema, String xml) throws SAXException, IOException, DocumentException {
        return readXML(new File(schema), new File(xml));
    }

    /**
     * 
     * Reads an XML file, validates it against a schema and returns a dom4j <code>Document</code>.
     * 
     * @param schemaFile
     *            The schema <code>File</code> which is intend to be used for validation.
     * @param xmlFile
     *            The XML <code>File</code> which is intend to be parsed.
     * @return a <code>Document</code> representation of the parsed file.
     * @throws SAXException
     *             if the specified XML file is not valid.
     * @throws IOException
     *             if an I/O exception occurs during validation.
     * @throws DocumentException
     *             if parsing the XML document failed.
     */
    public static Document readXML(File schemaFile, File xmlFile) throws SAXException, IOException, DocumentException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(xmlFile));

        return readXML(xmlFile);
    }

}

/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.xpathchooser.model;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Generates a simplified schema from a XML document.
 * 
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public final class XSDGenerator {
    
    /**
     * For index operations.
     */
    private static final int NOT_FOUND = -1;

    
    /**
     * Private constructor for utility class.
     */
    private XSDGenerator() {
    }

    /**
     * Generates a schema from a XML document.
     * 
     * @param filename The filename of the xml file.
     * @return The XSDRootElement for the given xml file.
     * @throws XMLStreamException Thrown if the XML document was invalid.
     * @throws IOException Thrown if the file could not be read.
     */
    public static XSDDocument generate(final String filename) throws XMLStreamException, IOException {
        final File file = new File(filename);
        return generate(file);
    }

    /**
     * Generates a schema from a XML document.
     * 
     * @param file The file of the xml file.
     * @return The XSDRootElement for the given xml file.
     * @throws XMLStreamException Thrown if the XML document was invalid.
     * @throws IOException Thrown if the file could not be read.
     */
    public static XSDDocument generate(final File file) throws XMLStreamException, IOException {
        final FileReader is = new FileReader(file);
        final XSDDocument xsdDocument = generate(is);
        is.close();
        return xsdDocument;
    }

    /**
     * Generates a schema from a XML document.
     * 
     * @param stream The input stream of the xml file.
     * @return The XSDRootElement for the given xml file.
     * @throws XMLStreamException Thrown if the XML document was invalid.
     */
    public static XSDDocument generate(final Reader stream) throws XMLStreamException {
        final XSDDocument document = new XSDDocument();
        XSDElement current = null;

        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLStreamReader parser = factory.createXMLStreamReader(stream);

        int curLevel = NOT_FOUND;
        final Map<Integer, List<XSDElement>> elementsOnLevel = new HashMap<Integer, List<XSDElement>>();
        elementsOnLevel.put(Integer.valueOf(curLevel + 1), new ArrayList<XSDElement>());
        
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.END_DOCUMENT) {
                parser.close();
                break;
            } else if (event == XMLStreamConstants.START_ELEMENT) {

                curLevel ++;
                List<XSDElement> levelList = elementsOnLevel.get(Integer.valueOf(curLevel));
                elementsOnLevel.put(Integer.valueOf(curLevel + 1), new ArrayList<XSDElement>());

                XSDElement element = new XSDElement(current, parser.getLocalName());
                
                if (current == null) {
                    document.getElements().add(element);
                } else {
                    int index = current.getElements().indexOf(element);
                    if (index != NOT_FOUND) {
                        element = current.getElements().get(index);
                    } else {
                        current.getElements().add(element);
                    }
                }
                current = element;

                if (!levelList.contains(element)) {
                    levelList.add(element);
                }

                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    
                    XSDAttribute attribute = new XSDAttribute(current, parser.getAttributeLocalName(i).toString());
                    int index = current.getAttributes().indexOf(attribute);
                    if (index != NOT_FOUND) {
                        attribute = current.getAttributes().get(index);
                    } else {
                        current.getAttributes().add(attribute);
                    }

                    final XSDValue attValue = new XSDValue(attribute, parser.getAttributeValue(i));
                    if (!attribute.getValues().contains(attValue)) {
                        attribute.getValues().add(attValue);
                    }
                }
                
            } else if (event == XMLStreamConstants.CHARACTERS) {
                final String tmp = parser.getText().trim();
                if (tmp.equals("")) {
                    continue;
                }

                final XSDValue value = new XSDValue(current, tmp);
                if (!current.getValues().contains(value)) {
                    current.getValues().add(value);
                }
                
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                current = (XSDElement) current.getParent();
                curLevel--;
            }

        }
        return document;
    }

}

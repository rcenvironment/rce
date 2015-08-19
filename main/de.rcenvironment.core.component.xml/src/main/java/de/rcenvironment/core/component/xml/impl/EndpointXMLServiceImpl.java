/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.xml.impl;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.xml.XMLComponentConstants;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.core.utils.incubator.xml.api.XMLSupportService;

/**
 * Default implementation of the EndpointXMLService.
 *
 * @author Brigitte Boden
 */
public class EndpointXMLServiceImpl implements EndpointXMLService {

    private static final String XML_FILE = "XML file ";

    private static final String DOT = ".";

    private static final String IN_FILE = " in file ";

    private final Log log = LogFactory.getLog(getClass());

    private XMLSupportService xmlSupport;

    /**
     * OSGI bind method.
     * 
     * @param service the service.
     */
    public void bindXMLSupportService(XMLSupportService service) {
        xmlSupport = service;
    }

    @Override
    public void updateXMLWithInputs(File xmlFile, Map<String, TypedDatum> dynamicInputs, ComponentContext componentContext)
        throws DataTypeException, ComponentException {
        if (dynamicInputs.isEmpty()) {
            // Nothing to do here
            return;
        }

        if (xmlFile == null) {
            throw new ComponentException("XML file does not exist.");
        }
        
        if (!xmlFile.exists()) {
            throw new ComponentException(XML_FILE + xmlFile.getAbsolutePath() + " does not exist.");
        }

        Document doc;
        try {
            doc = xmlSupport.readXMLFromFile(xmlFile);
        } catch (XMLException e) {
            throw new ComponentException(XML_FILE + xmlFile.getAbsolutePath() + " could not be read.", e);
        }
        for (final Entry<String, TypedDatum> entry : dynamicInputs.entrySet()) {
            final String xpath = componentContext.getInputMetaDataValue(entry.getKey(), XMLComponentConstants.CONFIG_KEY_XPATH);
            final TypedDatum value = entry.getValue();
            log.debug("Dynamic input " + entry.getKey() + ": Trying to replace xpath " + xpath + IN_FILE + xmlFile.getAbsolutePath()
                + " with new value " + value.toString());

            // Convert value to String;
            String valueAsString = getValueAsString(value);

            try {
                xmlSupport.replaceNodeText(doc, xpath, valueAsString);
                log.debug("Successfully replaced node text for dynamic input " + entry.getKey());
            } catch (XMLException e) {
                throw new ComponentException("Error while processing dynamic input " + entry.getKey() + DOT, e);
            }
        }
        try {
            xmlSupport.writeXMLtoFile(doc, xmlFile);
        } catch (XMLException e) {
            throw new ComponentException("Error while writing XML to file.", e);
        }
    }

    private String getValueAsString(final TypedDatum value) throws DataTypeException {
        switch (value.getDataType()) {
        case ShortText:
            return String.valueOf(((ShortTextTD) value).getShortTextValue());
        case Boolean:
            return String.valueOf(((BooleanTD) value).getBooleanValue());
        case Float:
            return String.valueOf(((FloatTD) value).getFloatValue());
        case Integer:
            return String.valueOf(((IntegerTD) value).getIntValue());
        default:
            throw new DataTypeException("Can not convert value (type: " + value.getDataType().getDisplayName()
                + ") to textual representation for insertion into XML.");
        }
    }

    @Override
    public void updateOutputsFromXML(File xmlFile, ComponentContext componentContext) throws DataTypeException,
        ComponentException {

        if (xmlFile == null) {
            throw new ComponentException("XML file does not exist.");
        }
        
        if (!xmlFile.exists()) {
            throw new ComponentException(XML_FILE + xmlFile.getAbsolutePath() + " does not exist.");
        }

        Document doc = null;

        for (String outputName : componentContext.getOutputs()) {
            if (componentContext.isDynamicOutput(outputName)) {

                //If the XML file has not been read, read it now.
                if (doc == null) {
                    try {
                        doc = xmlSupport.readXMLFromFile(xmlFile);
                    } catch (XMLException e) {
                        throw new ComponentException(XML_FILE + xmlFile.getAbsolutePath() + " could not be read.", e);
                    }
                }
                final String xpath = componentContext.getOutputMetaDataValue(outputName, XMLComponentConstants.CONFIG_KEY_XPATH);

                log.debug("Trying to evaluate xpath " + xpath + IN_FILE + xmlFile.getAbsolutePath() + " for output " + outputName);
                String valueAsString;
                final String message =
                    StringUtils.format(
                        "The value of XPath '%s' of output '%s' is not evaluable. Maybe XPath points to a node which does not exist.",
                        xpath, outputName);
                try {
                    valueAsString = xmlSupport.getElementText(doc, xpath);
                } catch (XMLException e) {
                    throw new ComponentException(message);
                }
                if (valueAsString == null) {
                    throw new ComponentException(message);
                }
                log.debug("Retreived raw value " + valueAsString + " for xpath " + xpath + IN_FILE + xmlFile.getAbsolutePath()
                    + " for output " + outputName + ", trying to convert to typed datum.");
                TypedDatum value = getValueAsTypedValue(outputName, valueAsString, componentContext);
                componentContext.writeOutput(outputName, value);
            }
        }
    }

    private TypedDatum getValueAsTypedValue(String outputName, String rawValue, ComponentContext componentContext)
        throws DataTypeException {
        TypedDatumFactory typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        TypedDatum value;
        try {
            switch (componentContext.getOutputDataType(outputName)) {
            case ShortText:
                value = typedDatumFactory.createShortText(rawValue);
                break;
            case Boolean:
                value = typedDatumFactory.createBoolean(Boolean.valueOf(rawValue));
                break;
            case Float:
                value = typedDatumFactory.createFloat(Double.valueOf(rawValue));
                break;
            case Integer:
                value = typedDatumFactory.createInteger(Long.valueOf(rawValue));
                break;
            default:
                throw new DataTypeException("Can not convert value \"" + rawValue + "\" to "
                    + componentContext.getOutputDataType(outputName).getDisplayName() + " for dynamic output \"" + outputName + "\"" + DOT);
            }
        } catch (NumberFormatException e) {
            throw new DataTypeException("Can not convert value \"" + rawValue + "\" to "
                + componentContext.getOutputDataType(outputName).getDisplayName() + " for dynamic output \"" + outputName + "\"" + DOT);
        }
        return value;
    }
}

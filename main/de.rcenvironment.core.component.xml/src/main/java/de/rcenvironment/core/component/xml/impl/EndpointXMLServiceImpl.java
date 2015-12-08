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
import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.core.utils.common.xml.XMLMapperConstants;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;

/**
 * Default implementation of the EndpointXMLService.
 *
 * @author Brigitte Boden
 */
public class EndpointXMLServiceImpl implements EndpointXMLService {

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

        if (!xmlFile.exists()) {
            throw new ComponentException(xmlFile.getAbsolutePath() + " does not exist");
        }
        synchronized (XMLMapperConstants.GLOBAL_MAPPING_LOCK) {
            Document doc;
            try {
                doc = xmlSupport.readXMLFromFile(xmlFile);
            } catch (XMLException e) {
                throw new ComponentException("Failed to read " + xmlFile.getAbsolutePath(), e);
            }
            for (final Entry<String, TypedDatum> entry : dynamicInputs.entrySet()) {
                final String xpath = componentContext.getInputMetaDataValue(entry.getKey(), XMLComponentConstants.CONFIG_KEY_XPATH);
                final TypedDatum value = entry.getValue();
    
                String valueAsString = getValueAsString(value);
                try {
                    xmlSupport.replaceNodeText(doc, xpath, valueAsString);
                    componentContext.getLog().componentInfo(StringUtils.format("Replaced value for '%s' with input value of '%s': %s",
                        xpath, entry.getKey(), value.toString()));
                } catch (XMLException e) {
                    throw new ComponentException(StringUtils.format("Failed to replace value for '%s' with value of input '%s': %s",
                        xpath, entry.getKey(), value.toString()), e);
                }
            }
            try {
                xmlSupport.writeXMLtoFile(doc, xmlFile);
            } catch (XMLException e) {
                throw new ComponentException("Failed to write XML content to file: " + xmlFile.getAbsolutePath(), e);
            }
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
            throw new DataTypeException("Can not convert value of type '" + value.getDataType().getDisplayName()
                + "' to textual representation for insertion into XML");
        }
    }

    @Override
    public void updateOutputsFromXML(File xmlFile, ComponentContext componentContext) throws DataTypeException,
        ComponentException {

        if (!xmlFile.exists()) {
            throw new ComponentException(xmlFile.getAbsolutePath() + " does not exist");
        }
        synchronized (XMLMapperConstants.GLOBAL_MAPPING_LOCK) {
            Document doc = null;
    
            for (String outputName : componentContext.getOutputs()) {
                if (componentContext.isDynamicOutput(outputName)) {
    
                    //If the XML file has not been read, read it now.
                    if (doc == null) {
                        try {
                            doc = xmlSupport.readXMLFromFile(xmlFile);
                        } catch (XMLException e) {
                            throw new ComponentException("Failed to read " + xmlFile.getAbsolutePath(), e);
                        }
                    }
                    final String xpath = componentContext.getOutputMetaDataValue(outputName, XMLComponentConstants.CONFIG_KEY_XPATH);
    
                    String valueAsString;
                    final String message = StringUtils.format("Failed to extract value for output '%s' that points to '%s'",
                        outputName, xpath);
                    try {
                        valueAsString = xmlSupport.getElementText(doc, xpath);
                    } catch (XMLException e) {
                        throw new ComponentException(message, e);
                    }
                    componentContext.getLog().componentInfo(
                        StringUtils.format("Extracted '%s' for XPath '%s' that will be sent to output '%s'",
                            valueAsString, xpath, outputName));
                    TypedDatum value = getValueAsTypedValue(outputName, valueAsString, componentContext);
                    componentContext.writeOutput(outputName, value);
                }
            }
        }
    }

    private TypedDatum getValueAsTypedValue(String outputName, String rawValue, ComponentContext componentContext)
        throws DataTypeException {
        String errorMessage = "Can not convert value '%s' to '%s' that is the required data type of output"
            + " '%s' this value should be sent to";
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
                throw new DataTypeException(StringUtils.format(errorMessage,
                    rawValue, componentContext.getOutputDataType(outputName).getDisplayName(), outputName));
            }
        } catch (NumberFormatException e) {
            throw new DataTypeException(StringUtils.format(errorMessage,
                rawValue, componentContext.getOutputDataType(outputName).getDisplayName(), outputName));
        }
        return value;
    }
}

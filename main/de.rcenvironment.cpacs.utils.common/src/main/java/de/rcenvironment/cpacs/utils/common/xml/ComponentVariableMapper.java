/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.utils.common.xml;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.incubator.XMLHelper;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;
import de.rcenvironment.cpacs.utils.common.components.CpacsChannelFilter;

/**
 * Handles the split and merge logic from cpacs components on rce-endpoints. Todo: Now the endpoint-name is the XPath. As soon as there are
 * metadata for endpoints, use them for xpath saving and handle different data types!
 * 
 * @author Markus Litz
 * @author Markus Kunde
 * @author Jan Flink
 */
public class ComponentVariableMapper {

    /**
     * Central logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(ComponentVariableMapper.class);

    private static TypedDatumFactory typedDatumFactory;

    private static final String STRING_SINGLE_QUOTE = "'";

    private static final String STRING_CANNOT_MAP_ON_EMPTY_OR_NULL_CPACS = "Cannot map on empty or null cpacs";

    private static final String STRING_ERROR_READING_XML_FROM_FILE = "Error reading XML from file";

    /** all input values of processing. */
    private Map<String, String> inputValues = new Hashtable<String, String>();

    /** all output values of processing. */
    private Map<String, String> outputValues = new Hashtable<String, String>();

    /**
     * Default constructor.
     */
    public ComponentVariableMapper() {}

    protected void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        typedDatumFactory = newTypedDatumService.getFactory();
    }

    protected void unbindTypedDatumService(TypedDatumService oldTypedDatumService) {
        // nothing to do here, but if this unbind method is missing, OSGi DS failed when disposing component
        // (seems to be a DS bug)
    }

    /**
     * Returns a string-serialized map of all input values.
     * 
     * @return input values
     */
    public Map<String, String> getInputValues() {
        return inputValues;
    }

    /**
     * Returns a string-serialized map of all output values.
     * 
     * @return output values
     */
    public Map<String, String> getOutputValues() {
        return outputValues;
    }

    /**
     * Updates a CPACS data set with values from channels that are dynamically added on configuration time.
     * 
     * @param cpacsFileName The path to the CPACS data set.
     * @param dynamicInputs The inputs to merge with the cpacs
     * @param componentContext The component context
     * @return true if success.
     * @throws DataTypeException thrown when data cannot cast to nodetext
     * @throws ComponentException thrown if xpath refers to no node
     */
    public boolean updateXMLWithInputs(final String cpacsFileName, final Map<String, TypedDatum> dynamicInputs,
        final ComponentContext componentContext) throws DataTypeException, ComponentException {
        if (cpacsFileName == null) {
            LOGGER.error(STRING_CANNOT_MAP_ON_EMPTY_OR_NULL_CPACS);
            return false;
        }

        final XMLHelper xmlHelper = new XMLHelper();
        Document doc = null;

        for (final Entry<String, TypedDatum> entry : dynamicInputs.entrySet()) {
            if (entry.getKey().equals(ChameleonCommonConstants.CHAMELEON_CPACS_NAME)
                || entry.getKey().equals(ChameleonCommonConstants.DIRECTORY_CHANNELNAME)) { // shouldn't happen (?) when using scheduler
                continue;
            }

            if (doc == null) {
                try {
                    doc = xmlHelper.readXMLFromFile(cpacsFileName);
                } catch (XMLException e) {
                    throw new ComponentException(STRING_ERROR_READING_XML_FROM_FILE, e);
                }
            }

            final String xpath = componentContext.getInputMetaDataValue(entry.getKey(), ChameleonCommonConstants.CHANNEL_XPATH);
            final TypedDatum value = entry.getValue();

            LOGGER.debug("ComponentVariableMapper::updateCPACSWithInputs - replacing '" + entry.getKey() + "' in CPACS '" + xpath
                + "' with new value '" + value + STRING_SINGLE_QUOTE);

            // Convert value to String;
            String valueAsString;
            switch (value.getDataType()) {
            case ShortText:
                valueAsString = String.valueOf(((ShortTextTD) value).getShortTextValue());
                break;
            case Boolean:
                valueAsString = String.valueOf(((BooleanTD) value).getBooleanValue());
                break;
            case Float:
                valueAsString = String.valueOf(((FloatTD) value).getFloatValue());
                break;
            case Integer:
                valueAsString = String.valueOf(((IntegerTD) value).getIntValue());
                break;
            default:
                throw new DataTypeException("Can not convert value (type: " + value.getDataType().getDisplayName()
                    + ") to textual representation for insertion into CPACS.");
            }

            try {
                if (!xmlHelper.replaceNodeText(doc, xpath, valueAsString)) {
                    LOGGER.error("ComponentVariableMapper::updateCPACSWithInputs. Error merging a value into CPACS.");
                    LOGGER.error("InputName = " + entry.getKey() + ", new Value: " + value.toString());
                }
                inputValues.put(xpath, valueAsString);
            } catch (IllegalArgumentException e) {
                final String message =
                    String.format("Error in component '%s'. XPath '%s' of input '%s' does not point to an existing node.",
                        componentContext.getInstanceName(), xpath, entry.getKey());
                LOGGER.error(message);
                throw new ComponentException(message, e);
            }
        }

        if (doc != null) {
            xmlHelper.writeXML(doc, cpacsFileName);
        }
        return true;
    }

    /**
     * Updates all output endpoints with data from a CPACS data set.
     * 
     * @param cpacsFileName The path to the CPACS data set.
     * @param outputName Name of affected output endpoint.
     * @param ci The componentInformation object from which the endpoints are read.
     * @return True if success
     * @throws DataTypeException thrown when nodetext cannot cast to output type
     * @throws ComponentException if xpath is not evaluable
     */
    public boolean updateOutputsFromXML(final String cpacsFileName, final String outputName,
        final ComponentContext ci) throws DataTypeException, ComponentException {
        if (cpacsFileName == null) {
            LOGGER.error(STRING_CANNOT_MAP_ON_EMPTY_OR_NULL_CPACS);
            return false;
        }
        final XMLHelper xmlHelper = new XMLHelper();
        Document doc = null;

        for (String output : ci.getOutputs()) {
            if (output.equals(outputName) || output.equals(ChameleonCommonConstants.DIRECTORY_CHANNELNAME)) {
                continue;
            }

            if (doc == null) {
                try {
                    doc = xmlHelper.readXMLFromFile(cpacsFileName);
                } catch (XMLException e) {
                    throw new ComponentException(STRING_ERROR_READING_XML_FROM_FILE, e);
                }
            }

            final String xpath = ci.getOutputMetaDataValue(output, ChameleonCommonConstants.CHANNEL_XPATH);

            String rawValue = xmlHelper.getElementText(doc, xpath, false);
            if (rawValue == null) {
                final String message =
                    String.format("Error in component '%s'."
                        + " The value of XPath '%s' of output '%s' is not evaluable. Maybe XPath points to a node which does not exist.",
                        xpath, output, outputName);
                LOGGER.error(message);
                throw new ComponentException(message);
            }
            TypedDatum value = getOutputValue(ci, output, rawValue);

            ci.writeOutput(output, value);
            LOGGER.debug("ComponentVariableMapper::updateOutputsFromCPACS - getting value '" + value
                + "' from node '" + output + STRING_SINGLE_QUOTE);
            outputValues.put(xpath, rawValue);
        }
        return true;
    }

    /**
     * Updates all output endpoints with data from a CPACS data set.
     * 
     * @param cpacsFileName The path to the CPACS data set.
     * @param componentContext The {@link ComponentContext} object from which the endpoints are read.
     * @return True if success
     * @throws DataTypeException thrown when nodetext cannot cast to output type
     * @throws ComponentException if xpath is not evaluable
     */
    public boolean updateOutputsFromCPACS(final String cpacsFileName, final ComponentContext componentContext) throws DataTypeException,
        ComponentException {
        if (cpacsFileName == null) {
            LOGGER.error(STRING_CANNOT_MAP_ON_EMPTY_OR_NULL_CPACS);
            return false;
        }
        final XMLHelper xmlHelper = new XMLHelper();
        Document doc = null;

        for (String output : componentContext.getOutputs()) {
            if (!CpacsChannelFilter.getVariableInputDataTypes().contains(componentContext.getOutputDataType(output))
                || componentContext.getDynamicOutputIdentifier(output) == null) {
                continue;
            }

            if (doc == null) {
                try {
                    doc = xmlHelper.readXMLFromFile(cpacsFileName);
                } catch (XMLException e) {
                    throw new ComponentException(STRING_ERROR_READING_XML_FROM_FILE, e);
                }
            }

            final String xpath = componentContext.getOutputMetaDataValue(output, ChameleonCommonConstants.CHANNEL_XPATH);

            String rawValue = xmlHelper.getElementText(doc, xpath, false);
            if (rawValue == null) {
                final String message =
                    String.format("Error in component '%s'."
                        + " The value of XPath '%s' of output '%s' is not evaluable. Maybe XPath points to a node which does not exist.",
                        xpath, componentContext.getInstanceName(), output);
                LOGGER.error(message);
                throw new ComponentException(message);
            }
            TypedDatum value = getOutputValue(componentContext, output, rawValue);

            componentContext.writeOutput(output, value);
            LOGGER.debug("ComponentVariableMapper::updateOutputsFromCPACS - getting value '" + value
                + "' from node '" + output + STRING_SINGLE_QUOTE);
            outputValues.put(xpath, rawValue);
        }
        return true;
    }

    private TypedDatum getOutputValue(ComponentContext componentContext, String outputName, String rawValue) throws DataTypeException {
        TypedDatum value;
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
            throw new DataTypeException("Can not convert CPACS-extracted value to "
                + componentContext.getOutputDataType(outputName).getDisplayName() + ".");
        }
        return value;
    }
}

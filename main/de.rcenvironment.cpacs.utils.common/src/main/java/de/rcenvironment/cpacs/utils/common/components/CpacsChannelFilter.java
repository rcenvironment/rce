/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.utils.common.components;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * Filtering channel-content for Cpacs components.
 * 
 * @author Markus Kunde
 * @author Jan Flink
 */
public final class CpacsChannelFilter {

    private static final Set<DataType> VARIABLE_INPUT_DATA_TYPES = new HashSet<DataType>(Arrays.asList(
        DataType.Integer,
        DataType.Boolean,
        DataType.Float,
        DataType.ShortText));

    /** Constructor. */
    private CpacsChannelFilter() {}

    public static Set<DataType> getVariableInputDataTypes() {
        return VARIABLE_INPUT_DATA_TYPES;
    }

    /**
     * Get cpacs value.
     * 
     * @param componentContext {@link ComponentContext} of calling component
     * @param inputName name of desired input
     * @return cpacs as file reference in data management
     */
    public static String getCpacs(String inputName, ComponentContext componentContext) {
        String cpacs = null;
        if (componentContext.getInputsWithDatum().contains(inputName)) {
            TypedDatum td = componentContext.readInput(inputName);
            if (td instanceof FileReferenceTD) {
                FileReferenceTD file = (FileReferenceTD) td;
                cpacs = file.getFileReference();
            }
        }

        return cpacs;
    }

    /**
     * Get directory value.
     * 
     * @param componentContext {@link ComponentContext} of calling component
     * @return directory as file reference in data management
     */
    public static String getDirectory(ComponentContext componentContext) {
        String directory = null;
        if (componentContext.getInputsWithDatum().contains(ChameleonCommonConstants.DIRECTORY_CHANNELNAME)) {
            TypedDatum td = componentContext.readInput(ChameleonCommonConstants.DIRECTORY_CHANNELNAME);
            if (td instanceof FileReferenceTD) {
                FileReferenceTD file = (FileReferenceTD) td;
                directory = file.getFileReference();
            }
        }
        return directory;
    }

    /**
     * Get all variable inputs with its first value. Delete them out of inputMap.
     * 
     * @param componentContext {@link ComponentContext} of calling component
     * @return input map with values
     */
    public static Map<String, TypedDatum> getVariableInputs(ComponentContext componentContext) {
        Map<String, TypedDatum> variableInputs = new Hashtable<String, TypedDatum>();

        for (String inputName : componentContext.getInputsWithDatum()) {
            if ((!inputName.equals(ChameleonCommonConstants.CHAMELEON_CPACS_NAME))
                && (!inputName.equals(ChameleonCommonConstants.DIRECTORY_CHANNELNAME))) {
                variableInputs.put(inputName, componentContext.readInput(inputName));
            }
        }
        
        return variableInputs;
    }

    /**
     * Get all variable inputs with its first value. Delete them out of inputMap.
     * 
     * @param inputMap map with all inputs and values
     * @param cpacsEndpointName name of incoming cpacs channel
     * @param componentContext {@link ComponentContext} of calling component
     * @return input map with values
     */
    public static Map<String, TypedDatum> getVariableInputs(Map<String, TypedDatum> inputMap, String cpacsEndpointName,
        ComponentContext componentContext) {
        Map<String, TypedDatum> variableInputs = new Hashtable<String, TypedDatum>();

        if (inputMap != null) {
            for (final Entry<String, TypedDatum> entry : inputMap.entrySet()) {

                if ((!entry.getKey().equals(cpacsEndpointName))
                    && (VARIABLE_INPUT_DATA_TYPES.contains(entry.getValue().getDataType()))
                    && componentContext.isDynamicInput(entry.getKey())) {

                    variableInputs.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return variableInputs;
    }
}

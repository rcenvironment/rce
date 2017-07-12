/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utils for all scripting elements.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel (#14029)
 * @author David Scholz (#14550, #14548)
 */
public final class ScriptingUtils {

    /**
     * Execution of Jython scripts must be synchronized with this lock object to ensure that only one Jython script is executed at the same
     * time within the entire JVM. The reason is that the Jython script engine is not thread safe (console outputs of multiple script
     * executions are mixed).
     */
    public static final Object SCRIPT_EVAL_LOCK_OBJECT = new Object();

    protected static final Log LOGGER = LogFactory.getLog(ScriptingUtils.class);

    private static final String JYTHON_JAR = "jython-standalone-2.5.1.jar";

    private static final String NOT_A_VALUE_UUID = "not_a_value_7fdc603e";

    private static final String USE_PYTHON_AS_SCRIPT_LANGUAGE_INSTEAD_STRING = " use Python as script language instead";

    private static String jythonPath = null;

    private static final String SLASH = "/";

    private static final String ESCAPESLASH = "\\\\";

    private static final String QUOTE = "\"";

    private static final String CLOSE_LIST_NEWLINE = "]\n";

    private static final String COMMA = ",";

    private static final int MAXIMUM_SMALL_TABLE_ENTRIES = 10000;

    private static TypedDatumFactory typedDatumFactory;

    private static ComponentDataManagementService componentDatamanagementService;

    public ScriptingUtils() {

    }

    /**
     * Determines the location of the jython.jar.
     * 
     * @throws IOException no valid file
     * @return path if found, else null
     */
    public static synchronized String getJythonPath() throws IOException {
        if (jythonPath == null) {
            // getting the Path where the Jython.jar is located. This is needed to
            // import Libraries to the jython script, e.g. os or re.
            String osgiConfigurationArea = System.getProperty("osgi.configuration.area");
            String decode = URLDecoder.decode(osgiConfigurationArea, "UTF-8");
            File osgiBundlestoreDir = null;
            if (decode.startsWith("file:/")) {
                osgiBundlestoreDir = new File(decode.substring(decode.indexOf("/")));
            }
            if (osgiBundlestoreDir != null) {
                jythonPath = findJythonPath(osgiBundlestoreDir);
            }
        }
        // return null;
        return jythonPath;
    }

    /**
     * Set jython path for tests.
     * 
     * @param path to set
     */
    public static synchronized void setJythonPath(String path) {
        jythonPath = path;
    }

    // /**
    // * Set JVM properties required for proper Jython 2.7.0 support.
    // */
    // public static void setJVMPropertiesForJython270Support() {
    // System.setProperty("python.import.site", "false");
    // System.setProperty("python.console.encoding", "UTF-8");
    // }

    /**
     * Determines the location of the jython.jar.
     * 
     * @param file directory
     * @throws IOException no valid file
     * @return path if found, else null
     */
    private static String findJythonPath(File file) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (File element : children) {
                String path = findJythonPath(element);
                if (path != null) {
                    return path;
                }
            }
        } else {
            String path = file.getAbsolutePath();
            path = path.replaceAll(ESCAPESLASH, SLASH);
            String[] splitted = path.split(SLASH);
            if (splitted[splitted.length - 1].equals(JYTHON_JAR)) {
                return path + "/Lib";
            }
        }
        return null;
    }

    /**
     * Prepares a header script for using RCE Python API with Jython.
     * 
     * @param localStateMap state map of the component
     * @param componentContext of the component
     * @param tempDir for creating temp files
     * @param tempFiles to delete removed
     * @return prepared header script
     * @throws ComponentException on unexpected error
     */
    public static String prepareHeaderScript(Map<String, Object> localStateMap, ComponentContext componentContext, File tempDir,
        List<File> tempFiles) throws ComponentException {
        String currentHeader = "";
        try (InputStream in = ScriptingUtils.class.getResourceAsStream("/resources/RCE_Jython.py")) {
            currentHeader = IOUtils.toString(in);
        } catch (IOException e) {
            throw new ComponentException("Internal error: Failed to intialize script that is wrapped around the actual script", e);
        }

        String stateMapDefinition = "RCE_STATE_VARIABLES = {";
        boolean first = true;
        for (String key : localStateMap.keySet()) {
            if (!first) {
                stateMapDefinition += COMMA;
            } else {
                first = false;
            }
            stateMapDefinition += QUOTE + key + "\" : " + localStateMap.get(key);
        }
        stateMapDefinition += "}";
        currentHeader += stateMapDefinition + "\n";
        // loading all input values
        currentHeader += prepareInput(tempDir, componentContext, tempFiles);
        String wrappingScript = "RCE_LIST_OUTPUTNAMES = [";

        first = true;
        for (String outputName : componentContext.getOutputs()) {
            if (!first) {
                wrappingScript += COMMA;
            } else {
                first = false;
            }
            wrappingScript += " \"" + outputName + QUOTE;
        }
        wrappingScript += "]\n";
        currentHeader += StringUtils.format("RCE_CURRENT_RUN_NUMBER = %s\n", componentContext.getExecutionCount());
        currentHeader += wrappingScript;
        currentHeader += "RCE.setDictionary_internal(RCE_Dict_InputChannels)\nimport shutil\n";
        List<String> notConnected = new LinkedList<>();
        for (String input : componentContext.getInputsNotConnected()) {
            if (componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null
                && (componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.RequiredIfConnected.name())
                    || componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)
                        .equals(
                            InputExecutionContraint.NotRequired.name()))) {
                notConnected.add(input);
            }
        }
        for (String input : componentContext.getInputs()) {
            if (componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null
                && componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.NotRequired.name())
                && !componentContext.getInputsWithDatum().contains(input)) {
                notConnected.add(input);
            }
        }
        String notConnectedValues = "[";
        for (String input : notConnected) {
            notConnectedValues += "\"" + input + "\",";
        }
        notConnectedValues.substring(0, notConnectedValues.length() - 1);
        notConnectedValues += "]";
        currentHeader += StringUtils.format("RCE_LIST_REQ_IF_CONNECTED_INPUTS = %s\n", notConnectedValues);
        return currentHeader;
    }

    private static String prepareInput(File tempDir, ComponentContext compContext, List<File> tempFiles) throws ComponentException {
        final String openBracket = "[";
        String dataDefinition = "RCE_Dict_InputChannels = { ";
        String nameAndValue = "";
        for (String inputName : compContext.getInputsWithDatum()) {
            nameAndValue = " \"" + StringEscapeUtils.escapeJava(inputName) + "\" : ";
            TypedDatum input = compContext.readInput(inputName);
            switch (compContext.getInputDataType(inputName)) {
            case FileReference:
                File fileInputDir = new File(tempDir, inputName);
                tempFiles.add(fileInputDir);
                File file = new File(fileInputDir, ((FileReferenceTD) input).getFileName());
                try {
                    // Since the code is shared for the script component and tool integration, there
                    // must be some difference here:
                    // In tool integration, copying the data is done by the component so at this
                    // point, it is not needed any more (is already exists)
                    // For the script component, this should always run.
                    if (!file.exists()) {
                        componentDatamanagementService.copyFileReferenceTDToLocalFile(compContext, (FileReferenceTD) input, file);
                    }
                } catch (IOException e) {
                    throw new ComponentException("Failed to read input file from the data management", e);
                }
                nameAndValue += QUOTE + file.getAbsolutePath().toString().replaceAll(ESCAPESLASH, SLASH) + QUOTE;
                break;
            case DirectoryReference:
                File dirInputDir = new File(tempDir, inputName);
                tempFiles.add(dirInputDir);
                File dir = new File(dirInputDir, ((DirectoryReferenceTD) input).getDirectoryName());
                try {
                    // see comment of file above
                    if (!dir.exists()) {
                        componentDatamanagementService.copyDirectoryReferenceTDToLocalDirectory(compContext,
                            (DirectoryReferenceTD) input, dirInputDir);
                    }
                } catch (IOException e) {
                    throw new ComponentException("Failed to read input directory from the data management", e);
                }
                nameAndValue += QUOTE + dir.getAbsolutePath().toString().replaceAll(ESCAPESLASH, SLASH) + QUOTE;
                break;
            case Boolean:
                boolean bool = (((BooleanTD) input).getBooleanValue());
                if (bool) {
                    nameAndValue += "True";
                } else {
                    nameAndValue += "False";
                }
                break;
            case ShortText:
                String value = ((ShortTextTD) input).getShortTextValue();
                if (value.contains("\n")) {
                    nameAndValue += QUOTE + QUOTE + QUOTE + StringEscapeUtils.escapeJava(value) + QUOTE + QUOTE + QUOTE;
                } else {
                    nameAndValue += QUOTE + StringEscapeUtils.escapeJava(value) + QUOTE;
                }
                break;
            case Integer:
                nameAndValue += input;
                break;
            case Float:
                String append = replaceNonNumericValue(((FloatTD) input).getFloatValue());
                if (append.isEmpty()) {
                    nameAndValue += ((FloatTD) input).getFloatValue();
                } else {
                    nameAndValue += append;
                }
                break;
            case Empty:
                nameAndValue = "None";
                break;
            case Vector:
                VectorTD vector = (VectorTD) input;
                nameAndValue += openBracket;
                if (vector.getRowDimension() > MAXIMUM_SMALL_TABLE_ENTRIES) {
                    throw new ComponentException(
                        StringUtils.format(
                            "Vector of input '%s' exceeds maximum number of entries allowed for Jython (entries: %s; maximum: %s);"
                                + USE_PYTHON_AS_SCRIPT_LANGUAGE_INSTEAD_STRING,
                            inputName,
                            vector.getRowDimension(), MAXIMUM_SMALL_TABLE_ENTRIES));
                }
                for (int i = 0; i < vector.getRowDimension(); i++) {
                    String appending = replaceNonNumericValue(vector.getFloatTDOfElement(i).getFloatValue());
                    if (appending.isEmpty()) {
                        nameAndValue += vector.getFloatTDOfElement(i).getFloatValue();
                    } else {
                        nameAndValue += appending;
                    }
                    nameAndValue += COMMA;
                }
                if (vector.getRowDimension() > 0) {
                    nameAndValue = nameAndValue.substring(0, nameAndValue.length() - 1);
                }
                nameAndValue += CLOSE_LIST_NEWLINE;
                break;

            case Matrix:
                MatrixTD matrix = (MatrixTD) input;
                nameAndValue += openBracket;
                nameAndValue = getMatrix(openBracket, nameAndValue, inputName, matrix);
                nameAndValue = nameAndValue.substring(0, nameAndValue.length() - 1);
                nameAndValue += CLOSE_LIST_NEWLINE;
                break;
            case SmallTable:
                nameAndValue = convertSmallTable(openBracket, nameAndValue, inputName, input);
                break;
            default:
                break;
            }
            dataDefinition += nameAndValue;
            // prepare next input.
            dataDefinition += " ,";
        }
        // deleting COMMA and close the dictionary
        dataDefinition = dataDefinition.substring(0, dataDefinition.length() - 1);
        dataDefinition += "}\n";
        return dataDefinition;

    }

    private static String convertSmallTable(final String openBracket, String nameAndValue, String inputName, TypedDatum input)
        throws ComponentException {
        SmallTableTD table = (SmallTableTD) input;
        nameAndValue += openBracket;
        if (table.getRowCount() * table.getColumnCount() > MAXIMUM_SMALL_TABLE_ENTRIES) {
            throw new ComponentException(
                StringUtils.format(
                    "Small Table of input '%s' exceeds maximum number of entries allowed for Jython (entries: %s; maximum: %s);"
                        + USE_PYTHON_AS_SCRIPT_LANGUAGE_INSTEAD_STRING,
                    inputName,
                    table.getRowCount() * table.getColumnCount(), MAXIMUM_SMALL_TABLE_ENTRIES));
        }
        for (int i = 0; i < table.getRowCount(); i++) {
            if (table.getRowCount() > 1) {
                nameAndValue += openBracket;
            }
            for (int j = 0; j < table.getColumnCount(); j++) {
                if (ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(
                    table.getTypedDatumOfCell(i, j)) instanceof String) {
                    nameAndValue += QUOTE
                        + ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(table.getTypedDatumOfCell(i, j))
                        + QUOTE + COMMA;
                } else if (ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(
                    table.getTypedDatumOfCell(i, j)) instanceof Double) {
                    nameAndValue += replaceNonNumericValue(((FloatTD) table.getTypedDatumOfCell(i, j)).getFloatValue()) + COMMA;
                } else {
                    nameAndValue +=
                        ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(table.getTypedDatumOfCell(i, j)) + COMMA;
                }
            }
            nameAndValue = nameAndValue.substring(0, nameAndValue.length() - 1);
            if (table.getRowCount() > 1) {
                nameAndValue += "],";
            } else {
                nameAndValue += COMMA;
            }
        }
        nameAndValue = nameAndValue.substring(0, nameAndValue.length() - 1);
        nameAndValue += CLOSE_LIST_NEWLINE;
        return nameAndValue;
    }

    private static String replaceNonNumericValue(double floatValue) {
        String result = "";
        if (floatValue == Double.NEGATIVE_INFINITY) {
            result += "float(\"-INF\")";
        } else if (floatValue == Double.POSITIVE_INFINITY) {
            result += "float(\"INF\")";
        } else if (Double.isNaN(floatValue)) {
            result += "float(\"nan\")";
        } else {
            result += floatValue;
        }
        return result;
    }

    private static String getMatrix(final String openBracket, String nameAndValue, String inputName, MatrixTD matrix)
        throws ComponentException {
        if (matrix.getRowDimension() * matrix.getColumnDimension() > MAXIMUM_SMALL_TABLE_ENTRIES) {
            throw new ComponentException(
                StringUtils.format(
                    "Small Table of input '%s' exceeds maximum number of entries allowed for Jython (entries: %s; maximum: %s);"
                        + USE_PYTHON_AS_SCRIPT_LANGUAGE_INSTEAD_STRING,
                    inputName,
                    matrix.getRowDimension() * matrix.getColumnDimension(), MAXIMUM_SMALL_TABLE_ENTRIES));
        }
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            if (matrix.getRowDimension() > 1) {
                nameAndValue += openBracket;
            }
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                if (ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(
                    matrix.getFloatTDOfElement(i, j)) instanceof String) {
                    nameAndValue += QUOTE
                        + ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(matrix.getFloatTDOfElement(i, j))
                        + QUOTE + COMMA;
                } else if (ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(
                    matrix.getFloatTDOfElement(i, j)) instanceof Double) {
                    String append = replaceNonNumericValue(matrix.getFloatTDOfElement(i, j).getFloatValue());
                    if (append.isEmpty()) {
                        nameAndValue += ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(matrix.getFloatTDOfElement(i, j));
                    } else {
                        nameAndValue += append;
                    }
                    nameAndValue += COMMA;
                } else {
                    nameAndValue +=
                        ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(matrix.getFloatTDOfElement(i, j)) + COMMA;
                }
            }
            nameAndValue = nameAndValue.substring(0, nameAndValue.length() - 1);
            if (matrix.getRowDimension() > 1) {
                nameAndValue += "],";
            } else {
                nameAndValue += COMMA;
            }
        }
        return nameAndValue;
    }

    /**
     * Write all output written with the RCE Script API.
     * 
     * @param stateMap current state map of script
     * @param componentContext from component
     * @param engine script engine
     * @param workingPath for files
     * @param historyDataItem of component instance
     * @throws ComponentException e if there is an error
     */
    public static void writeAPIOutput(Map<String, Object> stateMap, ComponentContext componentContext, ScriptEngine engine,
        String workingPath, ComponentHistoryDataItem historyDataItem)
        throws ComponentException {
        writeAPIOutput(stateMap, componentContext, engine, workingPath, historyDataItem, null);

    }

    /**
     * Write all output written with the RCE Script API.
     * 
     * @param stateMap current state map of script
     * @param componentContext from component
     * @param engine script engine
     * @param workingPath for files
     * @param historyDataItem of component instance
     * @param lastRunStaticOutputValues map if the outputs should be saved
     * @throws ComponentException e if there is an error
     */
    @SuppressWarnings("unchecked")
    public static void writeAPIOutput(Map<String, Object> stateMap, ComponentContext componentContext, ScriptEngine engine,
        String workingPath, ComponentHistoryDataItem historyDataItem, Map<String, TypedDatum> lastRunStaticOutputValues)
        throws ComponentException {
        Map<String, ArrayList<Object>> map = (Map<String, ArrayList<Object>>) engine.get("RCE_Dict_OutputChannels");
        // send values to outputs, using the Map
        // this block sends the values when the user calls the method RCE.write_output()
        for (String outputName : componentContext.getOutputs()) {

            DataType type = componentContext.getOutputDataType(outputName);
            List<Object> datas = map.get(outputName);
            if (datas != null) {
                for (Object value : datas) {
                    if (value != null && !String.valueOf(value).equals(NOT_A_VALUE_UUID)) {
                        TypedDatum outputValue = getOutputByType(value, type, outputName, workingPath, engine, componentContext);
                        writeOutput(componentContext, historyDataItem, lastRunStaticOutputValues, outputName, outputValue);
                    } else if (String.valueOf(value).equals(NOT_A_VALUE_UUID)) {
                        writeOutput(componentContext, historyDataItem, lastRunStaticOutputValues, outputName,
                            typedDatumFactory.createNotAValue());
                    }
                }
            }
        }

        Map<String, Object> stateMapOutput = (Map<String, Object>) engine.get("RCE_STATE_VARIABLES");
        for (String key : stateMapOutput.keySet()) {
            stateMap.put(key, stateMapOutput.get(key));
        }
        for (String endpointName : (List<String>) engine.get("RCE_CloseOutputChannelsList")) {
            componentContext.closeOutput(endpointName);
        }
    }

    private static void writeOutput(ComponentContext componentContext, ComponentHistoryDataItem historyDataItem,
        Map<String, TypedDatum> lastRunStaticOutputValues, String outputName, TypedDatum outputValue) {
        componentContext.writeOutput(outputName, outputValue);
        if (lastRunStaticOutputValues != null) {
            lastRunStaticOutputValues.put(outputName, outputValue);
        }
        addOutputToHistoryDataItem(outputName, outputValue, historyDataItem);
    }

    /**
     * @param engine the {@link ScriptEngine} executing the script which should be considered
     * @param componentContext the {@link ComponentContext} of the component
     * @return set with names of those output for which a not-a-value {@link TypedDatum} was written
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getOutputsSendingNotAValue(ScriptEngine engine, ComponentContext componentContext) {
        Map<String, ArrayList<Object>> map = (Map<String, ArrayList<Object>>) engine.get("RCE_Dict_OutputChannels");

        Set<String> returnSet = new HashSet<>();

        for (String outputName : componentContext.getOutputs()) {
            List<Object> datas = map.get(outputName);
            if (datas != null) {
                for (Object value : datas) {
                    if (value != null && String.valueOf(value).equals((NOT_A_VALUE_UUID))) {
                        returnSet.add(outputName);
                        break;
                    }
                }
            }
        }
        return returnSet;
    }

    @SuppressWarnings("unchecked")
    protected static TypedDatum getOutputByType(Object value, DataType type, String name, String workingPath, ScriptEngine engine,
        ComponentContext componentContext)
        throws ComponentException {
        TypedDatum outputValue = null;
        switch (type) {
        case ShortText:
            outputValue = typedDatumFactory.createShortText(value.toString());
            break;
        case Boolean:
            String stringValue = value.toString();
            boolean isNumber = true;

            try {
                float numberValue = Float.parseFloat(stringValue);
                if (Math.abs(numberValue) > 0) {
                    outputValue = typedDatumFactory.createBoolean(true);
                } else {
                    outputValue = typedDatumFactory.createBoolean(false);
                }
            } catch (NumberFormatException e) {
                isNumber = false;
            }

            if (!isNumber && (stringValue.equalsIgnoreCase("0") || stringValue.equalsIgnoreCase("0L") || stringValue.equalsIgnoreCase("0.0")
                || stringValue.equalsIgnoreCase("0j") || stringValue.equalsIgnoreCase("()") || stringValue.equalsIgnoreCase("[]")
                || stringValue.isEmpty() || stringValue.equalsIgnoreCase("{}") || stringValue.equalsIgnoreCase("false")
                || stringValue.equalsIgnoreCase("none"))) {

                outputValue = typedDatumFactory.createBoolean(false);

            } else if (!isNumber) {
                outputValue = typedDatumFactory.createBoolean(true);
            }
            break;
        case Float:
            try {
                outputValue = typedDatumFactory.createFloat(Double.parseDouble(value.toString()));
            } catch (NumberFormatException e) {
                throw new ComponentException(StringUtils.format("Failed to parse output value '%s' to data type Float."
                    + " Possible reasons (not restricted): Output value too big (max. %.1e),"
                    + " or output value contains non numeric characters.",
                    value.toString(), Double.MAX_VALUE));
            }

            break;
        case Integer:
            try {
                outputValue = typedDatumFactory.createInteger(Long.parseLong(value.toString()));
            } catch (NumberFormatException e) {
                throw new ComponentException(StringUtils.format("Failed to parse output value '%s' to data type Integer."
                    + " Possible reasons (not restricted): Output value too big (max. 2E"
                    + Long.toBinaryString(Long.MAX_VALUE).length() + " - 1),"
                    + " or output value contains non numeric characters.",
                    value.toString()));
            }

            break;
        case FileReference:
            outputValue = handeFileOrDirectoryOutput(value, "file", name, workingPath, componentContext, outputValue);
            break;
        case DirectoryReference:
            outputValue = handeFileOrDirectoryOutput(value, "directory ", name, workingPath, componentContext, outputValue);
            break;
        case Vector:
            VectorTD vector = null;
            List<Object> vectorRow = (List<Object>) value;
            vector = typedDatumFactory.createVector(vectorRow.size());
            int index = 0;
            for (Object element : vectorRow) {
                double convertedValue = 0;
                if (element instanceof List) {
                    throw new ComponentException(StringUtils
                        .format("Value for endpoint %s was a matrix, but endpoint is of type Vector", name));
                }
                if (element == null) {
                    throw new ComponentException(StringUtils
                        .format("Value \"None\" of cell %s is not valid for type Vector \"%s\"", index, name));
                } else if (element instanceof Integer) {
                    convertedValue = (Integer) element;
                } else {
                    convertedValue = (Double) element;
                }
                vector.setFloatTDForElement(typedDatumFactory.createFloat(convertedValue), index);
                index++;
            }
            outputValue = vector;
            break;
        case Matrix:
            outputValue = createResultMatrix(value, name);
            break;
        case SmallTable:
            TypedDatum[][] result = createResultArray(value);
            outputValue = typedDatumFactory.createSmallTable(result);
            break;
        default:
            outputValue = typedDatumFactory.createShortText(engine.get(name).toString());
            break;
        }
        return outputValue;
    }

    /**
     * Converts the output of the script to a {@link MatrixTD}.
     * 
     * @param value result of script
     * @param name of output
     * @return created matrix
     * @throws ComponentException if the dimensions are not correct.
     */
    @SuppressWarnings("unchecked")
    public static MatrixTD createResultMatrix(Object value, String name) throws ComponentException {
        if (!(value instanceof List)) {
            throw new ComponentException(StringUtils.format("Value \"%s\" of output \"%s\" is not of type matrix.", value, name));
        }
        List<Object> rowArray = (List<Object>) value;
        if (rowArray.size() > 0 && rowArray.get(0) instanceof List) {
            int columnDimension = ((List<Object>) rowArray.get(0)).size();
            MatrixTD matrix = typedDatumFactory.createMatrix(rowArray.size(), columnDimension);
            int i = 0;
            for (Object columnObject : rowArray) {
                if (!(columnObject instanceof List)) {
                    throw new ComponentException(
                        StringUtils.format("Value \"%s\" of output \"%s\" is not of type matrix.", columnObject, name));
                }
                List<Object> columnArray = (List<Object>) columnObject;
                if (columnArray.size() == 0) {
                    throw new ComponentException(StringUtils.format("Column %s of matrix \"%s\" does not contain any elements.", i, name));
                }
                if (columnArray.size() != columnDimension) {
                    throw new ComponentException(
                        StringUtils.format("Column %s of matrix %s has the wrong dimension (%s, should be %s).", i, name,
                            columnArray.size(), columnDimension));
                }
                int j = 0;
                for (Object element : columnArray) {
                    TypedDatum cellValue = ScriptDataTypeHelper.getTypedDatum(element, typedDatumFactory);
                    if (cellValue instanceof FloatTD) {
                        matrix.setFloatTDForElement((FloatTD) cellValue, i, j++);
                    } else if (cellValue instanceof IntegerTD) {
                        matrix.setFloatTDForElement(typedDatumFactory.createFloat(((IntegerTD) cellValue).getIntValue()), i, j++);
                    } else if (cellValue instanceof ShortTextTD && ((ShortTextTD) cellValue).getShortTextValue().equals("+Infinity")) {
                        matrix.setFloatTDForElement(typedDatumFactory.createFloat(Double.POSITIVE_INFINITY), i, j++);
                    } else {
                        String elementString = "None";
                        if (element != null) {
                            elementString = element.toString();
                        }
                        throw new ComponentException(
                            StringUtils.format("Value \"%s\" of cell (%s, %s) of matrix \"%s\" is not an integer or a float value.",
                                elementString, i, j, name));
                    }
                }
                i++;
            }
            return matrix;
        }
        throw new ComponentException(StringUtils.format("Dimensions of %s not correct to convert to a Matrix", name));
    }

    @SuppressWarnings("unchecked")
    private static TypedDatum[][] createResultArray(Object value) throws ComponentException {
        if (!(value instanceof List)) {
            throw new ComponentException(StringUtils.format("Value \"%s\" is not of type small table.", value));
        }
        List<Object> rowArray = (List<Object>) value;
        TypedDatum[][] result = new TypedDatum[rowArray.size()][];
        Object first = "";
        if (rowArray.size() > 0 && rowArray.get(0) instanceof List) {
            int i = 0;
            int size = 0;
            for (Object columnObject : rowArray) {
                if (!(columnObject instanceof List)) {
                    throw new ComponentException(StringUtils.format("Value \"%s\" is not of type small table.", columnObject));
                }
                List<Object> columnArray = (List<Object>) columnObject;
                if (size == 0) {
                    first = columnObject;
                    size = columnArray.size();
                }

                if (size != columnArray.size()) {
                    throw new ComponentException(StringUtils.format(
                        "Each row must have the same number of elements in a small table. "
                            + "Element count of \"%s\" and \"%s\" does not match.",
                        first, columnObject));
                }

                if (columnArray.size() == 0) {
                    result[i] = new TypedDatum[1];
                    result[i][0] = typedDatumFactory.createEmpty();
                } else {
                    result[i] = new TypedDatum[columnArray.size()];
                }

                int j = 0;
                for (Object element : columnArray) {
                    result[i][j++] = ScriptDataTypeHelper.getTypedDatum(element, typedDatumFactory);
                }
                i++;
            }
        } else {
            int i = 0;
            // ??
            for (Object element : rowArray) {
                result[i] = new TypedDatum[1];
                result[i][0] = ScriptDataTypeHelper.getTypedDatum(element, typedDatumFactory);
                i++;
            }
        }
        return result;
    }

    private static TypedDatum handeFileOrDirectoryOutput(Object value, String type, String name, String workingPath,
        ComponentContext componentContext, TypedDatum outputValue) throws ComponentException {
        try {
            File file = new File(value.toString());
            if (!file.isAbsolute()) {
                file = new File(workingPath, value.toString());
            }
            if (file.exists()) {
                if (type.equals("file")) {
                    if (file.isDirectory()) {
                        throw new ComponentException(StringUtils.format(
                            "Failed to write %s to output '%s' as it is a directory.", file.getAbsolutePath(), name));
                    }
                    outputValue = componentDatamanagementService.createFileReferenceTDFromLocalFile(
                        componentContext, file, file.getName());
                } else {
                    if (!file.isDirectory()) {
                        throw new ComponentException(StringUtils.format(
                            "Failed to write %s to output '%s' as it is not a directory.", file.getAbsolutePath(), name));
                    }
                    outputValue = componentDatamanagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext, file,
                        file.getName());
                }
            } else {
                throw new ComponentException(StringUtils.format(
                    "Failed to write %s to output '%s' as it does not exist: %s", type, name, file.getAbsolutePath()));
            }
        } catch (IOException e) {
            throw new ComponentException(StringUtils.format("Failed to store %s into the data management - "
                + "if it is not stored in the data management, it can not be sent as output value", type), e);
        }
        return outputValue;
    }

    private static void addOutputToHistoryDataItem(String name, TypedDatum outputValue, ComponentHistoryDataItem historyDataItem) {
        if (historyDataItem != null) {
            ((CommonComponentHistoryDataItem) historyDataItem).addOutput(name, outputValue);
        }
    }

    /**
     * OSGI method.
     * 
     * @param newTypedDatumService new service
     */
    public void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        typedDatumFactory = newTypedDatumService.getFactory();
    }

    /**
     * OSGI method.
     * 
     * @param oldTypedDatumService new service
     */
    public void unbindTypedDatumService(TypedDatumService oldTypedDatumService) {
        /*
         * nothing to do here, this unbind method is only needed, because DS is throwing an exception when disposing otherwise. probably a
         * bug
         */
    }

    /**
     * OSGI method.
     * 
     * @param compDataManagementService new service
     */
    public void bindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        componentDatamanagementService = compDataManagementService;
    }

    /**
     * OSGI method.
     * 
     * @param compDataManagementService new service
     */
    public void unbindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        componentDatamanagementService = null;
    }
}

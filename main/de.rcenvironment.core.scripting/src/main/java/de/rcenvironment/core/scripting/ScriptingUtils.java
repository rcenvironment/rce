/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utils for all scripting elements.
 * 
 * @author Sascha Zur
 */
public final class ScriptingUtils {

    /**
     * Execution of Jython scripts must be synchronized with this lock object to ensure that only
     * one Jython script is executed at the same time within the entire JVM. The reason is that the
     * Jython script engine is not thread safe (console outputs of multiple script executions are
     * mixed).
     */
    public static final Object SCRIPT_EVAL_LOCK_OBJECT = new Object();

    protected static final Log LOGGER = LogFactory.getLog(ScriptingUtils.class);

    private static String jythonPath = null;

    private static final String SLASH = "/";

    private static final String ESCAPESLASH = "\\\\";

    private static final String QUOTE = "\"";

    private static final String CLOSE_LIST_NEWLINE = "]\n";

    private static final String COMMA = ",";

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
            String osgiBundlestore = System.getProperty("osgi.bundlestore");
            File osgiBundlestoreDir = new File(osgiBundlestore.replaceAll(ESCAPESLASH, SLASH));
            jythonPath = findJythonPath(osgiBundlestoreDir);
        }
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
            if (splitted[splitted.length - 1].equals("jython-standalone-2.5.1.jar")) {
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
     */
    public static String prepareHeaderScript(Map<String, Object> localStateMap, ComponentContext componentContext, File tempDir,
        List<File> tempFiles) {
        InputStream in = ScriptingUtils.class.getResourceAsStream("/resources/RCE_Jython.py");
        String currentHeader = "";
        try {
            currentHeader = IOUtils.toString(in);
        } catch (IOException e) {
            LOGGER.error(e);
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
        List<String> notConnected = new LinkedList<String>();
        for (String input : componentContext.getInputsNotConnected()) {
            if (componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null
                && (componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.RequiredIfConnected.name())
                || componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.NotRequired.name()))) {
                notConnected.add(input);
            }
        }
        for (String input : componentContext.getInputs()) {
            if (componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null
                && componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.NotRequired.name()) && !componentContext.getInputsWithDatum().contains(input)) {
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

    private static String prepareInput(File tempDir, ComponentContext compContext, List<File> tempFiles) {
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
                    throw new RuntimeException("Failed to load input file from the data management", e);
                }
                nameAndValue += QUOTE + file.getAbsolutePath().toString().replaceAll(ESCAPESLASH, SLASH) + QUOTE;
                break;
            case DirectoryReference:
                File dirInputDir = new File(tempDir, inputName);
                tempFiles.add(dirInputDir);
                File dir = new File(dirInputDir, ((DirectoryReferenceTD) input).getDirectoryName());
                try {
                    // Since the code is shared for the script component and tool integration, there
                    // must be some difference here:
                    // In tool integration, copying the data is done by the component so at this
                    // point, it is not needed any more (is already exists)
                    // For the script component, this should always run.
                    if (!dir.exists()) {
                        componentDatamanagementService.copyDirectoryReferenceTDToLocalDirectory(compContext,
                            (DirectoryReferenceTD) input, dirInputDir);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load input directory from the data management", e);
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
                nameAndValue += input;
                break;
            case Empty:
                nameAndValue = "None";
                break;
            case Vector:
                VectorTD vector = (VectorTD) input;
                nameAndValue += openBracket;
                for (int i = 0; i < vector.getRowDimension(); i++) {
                    nameAndValue += vector.getFloatTDOfElement(i).getFloatValue() + COMMA;
                }
                if (vector.getRowDimension() > 0) {
                    nameAndValue = nameAndValue.substring(0, nameAndValue.length() - 1);
                }
                nameAndValue += CLOSE_LIST_NEWLINE;
                break;
            case SmallTable:
                SmallTableTD table = (SmallTableTD) input;
                nameAndValue += openBracket;
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

    /**
     * Write all output written with the RCE Script API.
     * 
     * @param stateMap current state map of script
     * @param componentContext from component
     * @param engine script engine
     * @param workingPath for files
     * @param historyDataItem of component instance
     * @throws ComponentException e
     */
    @SuppressWarnings("unchecked")
    public static void writeAPIOutput(Map<String, Object> stateMap, ComponentContext componentContext, ScriptEngine engine,
        String workingPath, ComponentHistoryDataItem historyDataItem) throws ComponentException {
        Map<String, ArrayList<Object>> map = (Map<String, ArrayList<Object>>) engine.get("RCE_Dict_OutputChannels");
        // send values to outputs, using the Map
        // this block sends the values when the user calls the method RCE.write_output()
        for (String outputName : componentContext.getOutputs()) {
            DataType type = componentContext.getOutputDataType(outputName);
            List<Object> datas = map.get(outputName);
            if (datas != null) {
                for (Object value : datas) {
                    if (value != null) {
                        writeOutputByType(value, type, outputName, workingPath, engine, historyDataItem, componentContext);
                    }
                }
            }
        }

        if (engine.get("RCE_NotAValueOutputList") != null) {
            for (String endpointName : (List<String>) engine.get("RCE_NotAValueOutputList")) {
                componentContext.writeOutput(endpointName, typedDatumFactory.createNotAValue());
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

    @SuppressWarnings("unchecked")
    protected static void writeOutputByType(Object value, DataType type, String name, String workingPath, ScriptEngine engine,
        ComponentHistoryDataItem historyDataItem, ComponentContext componentContext)
        throws ComponentException {
        TypedDatum outputValue = null;
        switch (type) {
        case ShortText:
            outputValue = typedDatumFactory.createShortText(value.toString());
            break;
        case Boolean:
            outputValue = typedDatumFactory.createBoolean(Boolean.parseBoolean(value.toString()));
            break;
        case Float:
            try {
                outputValue = typedDatumFactory.createFloat(Double.parseDouble(value.toString()));
            } catch (NumberFormatException e) {
                LOGGER.error(StringUtils.format("Output %s could not be parsed to data type Float", value.toString()));
                throw new ComponentException(e);
            }

            break;
        case Integer:
            try {
                outputValue = typedDatumFactory.createInteger(Long.parseLong(value.toString()));
            } catch (NumberFormatException e) {
                LOGGER.error(StringUtils.format("Output %s could not be parsed to data type Integer", value.toString()));
                throw new ComponentException(e);
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
                if (element instanceof Integer) {
                    convertedValue = (Integer) element;
                } else {
                    convertedValue = (Double) element;
                }
                vector.setFloatTDForElement(typedDatumFactory.createFloat(convertedValue), index);
                index++;
            }
            outputValue = vector;
            break;
        case SmallTable:
            List<Object> rowArray = (List<Object>) value;
            TypedDatum[][] result = new TypedDatum[rowArray.size()][];
            if (rowArray.size() > 0 && rowArray.get(0) instanceof List) {
                int i = 0;
                for (Object columnObject : rowArray) {
                    List<Object> columnArray = (List<Object>) columnObject;
                    result[i] = new TypedDatum[columnArray.size()];
                    int j = 0;
                    for (Object element : columnArray) {
                        result[i][j++] = ScriptDataTypeHelper.getTypedDatum(element, typedDatumFactory);
                    }
                    i++;
                }
                outputValue = typedDatumFactory.createSmallTable(result);
            } else {
                int i = 0;
                for (Object element : rowArray) {
                    result[i] = new TypedDatum[1];
                    result[i][0] = ScriptDataTypeHelper.getTypedDatum(element, typedDatumFactory);
                    i++;
                }
                outputValue = typedDatumFactory.createSmallTable(result);
            }
            break;
        default:
            outputValue = typedDatumFactory.createShortText(engine.get(name).toString());
            break;
        }

        componentContext.writeOutput(name, outputValue);
        addOutputToHistoryDataItem(name, outputValue, historyDataItem);
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
                    outputValue =
                        componentDatamanagementService.createFileReferenceTDFromLocalFile(componentContext, file, file.getName());
                } else {
                    outputValue = componentDatamanagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext, file,
                        file.getName());
                }
            } else {
                throw new ComponentException(StringUtils.format(
                    "Could not write %s for output \"%s\" because it does not exist: %s", type, name, file.getAbsolutePath()));
            }
        } catch (IOException ex) {
            throw new ComponentException("Failed to write FileReference to output Channel", ex);
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
         * nothing to do here, this unbind method is only needed, because DS is throwing an
         * exception when disposing otherwise. probably a bug
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

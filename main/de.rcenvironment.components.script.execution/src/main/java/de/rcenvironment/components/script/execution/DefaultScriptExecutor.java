/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.script.execution;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.history.ComponentHistoryDataItemConstants;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.scripting.WorkflowConsoleForwardingWriter;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.scripting.ScriptDataTypeHelper;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Implementation of {@link ScriptExecutor} to execute default script engines.
 * 
 * @author Mark Geiger
 * @author Sascha Zur
 */
public class DefaultScriptExecutor implements ScriptExecutor {

    protected static TypedDatumFactory typedDatumFactory;

    protected static ScriptingService scriptingService;

    protected static final String INPUT_STRING = " = \"%s\"\n";

    protected static final String INPUT_NO_STRING = " = %s \n";

    protected static final Log LOGGER = LogFactory.getLog(DefaultScriptExecutor.class);

    protected static ComponentDataManagementService componentDatamanagementService;

    protected ComponentContext componentContext;

    protected ScriptEngine scriptEngine;

    protected String wrappingScript;

    protected List<File> tempFiles = new LinkedList<File>();

    protected DistributedNotificationService notificationService;

    protected File tempDir;

    protected ScriptComponentHistoryDataItem historyDataItem;

    private File stderrLogFile;

    private File stdoutLogFile;

    private Writer stdoutWriter;

    private Writer stderrWriter;

    /**
     * Resets the script component for being used in nested loops.
     */
    @Override
    public void reset() {

    }

    protected void bindScriptingService(final ScriptingService service) {
        scriptingService = service;
    }

    protected void unbindScriptingService(final ScriptingService service) {
        /*
         * nothing to do here, this unbind method is only needed, because DS is throwing an
         * exception when disposing otherwise. probably a bug
         */
    }

    protected void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        typedDatumFactory = newTypedDatumService.getFactory();
    }

    protected void unbindTypedDatumService(TypedDatumService noldTypedDatumService) {
        /*
         * nothing to do here, this unbind method is only needed, because DS is throwing an
         * exception when disposing otherwise. probably a bug
         */
    }

    @Override
    public boolean prepareExecutor(ComponentContext compCtx, DistributedNotificationService inNotificationService) {
        notificationService = inNotificationService;
        this.componentContext = compCtx;
        try {
            tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("scriptExecutor");
        } catch (IOException e) {
            LOGGER.error("Creating temp directory failed", e);
        }
        return false;
    }

    @Override
    public void prepareNewRun(ScriptLanguage scriptLanguage, String userScript,
        ScriptComponentHistoryDataItem dataItem) throws ComponentException {}

    @Override
    public void prepareOutputForRun() {
        final int buffer = 1024;
        StringWriter out = new StringWriter(buffer);
        StringWriter err = new StringWriter(buffer);

        initializeLogFileForHistoryDataItem();

        stdoutWriter = new WorkflowConsoleForwardingWriter(out, componentContext, ConsoleRow.Type.STDOUT, stdoutLogFile);
        stderrWriter = new WorkflowConsoleForwardingWriter(err, componentContext, ConsoleRow.Type.STDERR, stderrLogFile);

        scriptEngine.getContext().setWriter(stdoutWriter);
        scriptEngine.getContext().setErrorWriter(stderrWriter);
    }

    private void initializeLogFileForHistoryDataItem() {
        if (historyDataItem != null) {
            try {
                stdoutLogFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                    ComponentHistoryDataItemConstants.STDOUT_LOGFILE_NAME);
            } catch (IOException e) {
                LOGGER.error("Creating temp file for console output failed. No log file will be added to component history data", e);
            }
            try {
                stderrLogFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                    ComponentHistoryDataItemConstants.STDERR_LOGFILE_NAME);
            } catch (IOException e) {
                LOGGER.error("Creating temp file for console error output failed. No log file will be added to component history data", e);
            }
        }
    }

    @Override
    public void runScript() throws ComponentException {
        prepareOutputForRun();

        setScriptToHistoryDataItem();
        try {
            // execute script here
            scriptEngine.eval(wrappingScript);
        } catch (ScriptException e) {
            throw new ComponentException("Could not run script. Maybe the script has errors? \n\n: " + e.toString());
        }

    }

    private void setScriptToHistoryDataItem() {
        if (historyDataItem != null) {
            try {
                String scriptFileRef = componentDatamanagementService.createTaggedReferenceFromString(componentContext, wrappingScript);
                historyDataItem.setScriptFileReference(scriptFileRef);
            } catch (IOException e) {
                LOGGER.error("Adding Python script to component history data failed", e);
            }
        }
    }

    public long getCurrentRunNumber() {
        return componentContext.getExecutionCount();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean postRun() throws ComponentException {

        // send values to outputs
        for (String outputName : componentContext.getOutputs()) {
            DataType type = componentContext.getOutputDataType(outputName);
            String value = "";
            TypedDatum outputValue;
            if (scriptEngine.get(outputName) != null) {
                switch (type) {
                case ShortText:
                    value = String.valueOf(scriptEngine.get(outputName));
                    outputValue = typedDatumFactory.createShortText(value);
                    break;
                case Boolean:
                    value = String.valueOf(scriptEngine.get(outputName));
                    outputValue = typedDatumFactory.createBoolean(Boolean.parseBoolean(value));
                    break;
                case Float:
                    value = String.valueOf(scriptEngine.get(outputName));
                    outputValue = typedDatumFactory.createFloat(Double.parseDouble(value));
                    break;
                case Integer:
                    value = String.valueOf(scriptEngine.get(outputName));
                    outputValue = typedDatumFactory.createInteger(Long.parseLong(value));
                    break;
                case FileReference:
                    value = String.valueOf(scriptEngine.get(outputName));
                    try {
                        File file = new File(value);
                        outputValue = componentDatamanagementService.createFileReferenceTDFromLocalFile(componentContext, file,
                            file.getName());
                        tempFiles.add(file);
                    } catch (IOException e) {
                        throw new ComponentException(String.format(
                            "Storing file in the data management failed. No file is written to output '%s'", outputName), e);
                    }
                    break;
                case DirectoryReference:
                    value = String.valueOf(scriptEngine.get(outputName));
                    try {
                        File file = new File(value);
                        outputValue = componentDatamanagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext,
                            file, file.getName());
                        tempFiles.add(file);
                    } catch (IOException e) {
                        throw new ComponentException(String.format(
                            "Storing directory in the data management failed. No directory is written to output '%s'", outputName),
                            e);
                    }
                    break;
                case SmallTable:
                    List<Object> rowArray = (List<Object>) scriptEngine.get(outputName);
                    TypedDatum[][] result = new TypedDatum[rowArray.size()][];
                    if (rowArray.size() > 0 && rowArray.get(0) instanceof List) {
                        int i = 0;
                        for (Object columnObject : rowArray) {
                            List<Object> columnArray = (List<Object>) columnObject;
                            result[i] = new TypedDatum[columnArray.size()];
                            int j = 0;
                            for (Object element : columnArray) {
                                result[i][j++] = getTypedDatum(element);
                            }
                            i++;
                        }
                        outputValue = typedDatumFactory.createSmallTable(result);
                    } else {
                        int i = 0;
                        for (Object element : rowArray) {
                            result[i] = new TypedDatum[1];
                            result[i][0] = getTypedDatum(element);
                            i++;
                        }
                        outputValue = typedDatumFactory.createSmallTable(result);
                    }
                    break;
                default:
                    outputValue = typedDatumFactory.createShortText(scriptEngine.get(outputName).toString()); // should
                                                                                                              // not
                                                                                                              // happen
                    break;
                }
                componentContext.writeOutput(outputName, outputValue);
            }
        }

        try {
            closeConsoleWritersAndAddLogsToHistoryDataItem();
        } catch (IOException e) {
            throw new ComponentException(e);
        }

        return true;
    }

    protected void closeConsoleWritersAndAddLogsToHistoryDataItem() throws IOException {
        stderrWriter.flush();
        stderrWriter.close();
        stdoutWriter.flush();
        stdoutWriter.close();

        if (historyDataItem != null) {
            if (!FileUtils.readFileToString(stderrLogFile).isEmpty()) {
                String stderrFileRef = componentDatamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    stderrLogFile, stderrLogFile.getName());
                historyDataItem.addLog(stderrLogFile.getName(), stderrFileRef);
            }
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stderrLogFile);
            if (!FileUtils.readFileToString(stdoutLogFile).isEmpty()) {
                String stdoutFileRef = componentDatamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    stdoutLogFile, stdoutLogFile.getName());
                historyDataItem.addLog(stdoutLogFile.getName(), stdoutFileRef);
            }
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stdoutLogFile);
        }
    }

    protected void bindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        componentDatamanagementService = compDataManagementService;
    }

    protected TypedDatum getTypedDatum(Object value) {
        return ScriptDataTypeHelper.getTypedDatum(value, typedDatumFactory);
    }

    @Override
    public void deleteTempFiles() {
        // delete all temporary created files
        try {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempDir);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage() + " : Perhaps, a file was not closed in the script?");
        }

    }

}

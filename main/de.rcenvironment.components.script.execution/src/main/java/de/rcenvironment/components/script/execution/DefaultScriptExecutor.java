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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;

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
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
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
public abstract class DefaultScriptExecutor implements ScriptExecutor {

    protected static TypedDatumFactory typedDatumFactory;

    protected static ScriptingService scriptingService;

    protected static final String INPUT_STRING = " = \"%s\"\n";

    protected static final String INPUT_NO_STRING = " = %s \n";

    private static final Log LOGGER = LogFactory.getLog(DefaultScriptExecutor.class);

    protected ComponentContext componentContext;

    protected ScriptEngine scriptEngine;

    protected String wrappingScript;

    protected List<File> tempFiles = new LinkedList<File>();

    protected File tempDir;

    protected ScriptComponentHistoryDataItem historyDataItem;

    protected Map<String, Object> stateMap;

    private File stderrLogFile;

    private File stdoutLogFile;

    private Writer stdoutWriter;

    private Writer stderrWriter;

    /**
     * Resets the script component for being used in nested loops.
     */
    @Override
    public void reset() {
        stateMap = new HashMap<String, Object>();
    }

    @Override
    public boolean prepareExecutor(ComponentContext compCtx) {
        this.componentContext = compCtx;
        try {
            tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("scriptExecutor");
        } catch (IOException e) {
            LOGGER.error("Creating temp directory failed", e);
        }
        return false;
    }

    @Override
    public abstract void prepareNewRun(ScriptLanguage scriptLanguage, String userScript,
        ScriptComponentHistoryDataItem dataItem) throws ComponentException;

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
    public abstract void runScript() throws ComponentException;

    public long getCurrentRunNumber() {
        return componentContext.getExecutionCount();
    }

    @Override
    public abstract boolean postRun() throws ComponentException;

    protected void closeConsoleWritersAndAddLogsToHistoryDataItem() throws IOException {
        stderrWriter.flush();
        stderrWriter.close();
        stdoutWriter.flush();
        stdoutWriter.close();

        if (historyDataItem != null) {
            ComponentDataManagementService componentDataManagementService =
                componentContext.getService(ComponentDataManagementService.class);
            if (!FileUtils.readFileToString(stderrLogFile).isEmpty()) {
                String stderrFileRef = componentDataManagementService.createTaggedReferenceFromLocalFile(componentContext,
                    stderrLogFile, stderrLogFile.getName());
                historyDataItem.addLog(stderrLogFile.getName(), stderrFileRef);
            }
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stderrLogFile);
            if (!FileUtils.readFileToString(stdoutLogFile).isEmpty()) {
                String stdoutFileRef =
                    componentDataManagementService.createTaggedReferenceFromLocalFile(componentContext,
                        stdoutLogFile, stdoutLogFile.getName());
                historyDataItem.addLog(stdoutLogFile.getName(), stdoutFileRef);
            }
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stdoutLogFile);
        }
    }

    protected TypedDatum getTypedDatum(Object value) {
        return ScriptDataTypeHelper.getTypedDatum(value, componentContext.getService(TypedDatumService.class).getFactory());
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

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Override
    public void setHistoryDataItem(ScriptComponentHistoryDataItem historyDataItem) {
        this.historyDataItem = historyDataItem;
    }

    @Override
    public void setStateMap(Map<String, Object> stateMap) {
        this.stateMap = stateMap;
    }

    @Override
    public void setStdoutLogFile(File stdoutLogFile) {
        this.stdoutLogFile = stdoutLogFile;
    }

    @Override
    public void setStderrLogFile(File stderrLogFile) {
        this.stderrLogFile = stderrLogFile;
    }

    @Override
    public void setStdoutWriter(Writer stdoutWriter) {
        this.stdoutWriter = stdoutWriter;
    }

    @Override
    public void setStderrWriter(Writer stderrWriter) {
        this.stderrWriter = stderrWriter;
    }

    @Override
    public void setWorkingPath(String path) {}
}

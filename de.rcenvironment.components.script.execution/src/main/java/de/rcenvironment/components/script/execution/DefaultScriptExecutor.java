/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.scripting.WorkflowConsoleForwardingWriter;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
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
    public boolean prepareExecutor(ComponentContext compCtx) throws ComponentException {
        this.componentContext = compCtx;
        try {
            tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("scriptExecutor");
        } catch (IOException e) {
            throw new ComponentException("Failed to create temporary directory needed to temporarely store input files/directories", e);
        }
        return true;
    }

    @Override
    public abstract void prepareNewRun(ScriptLanguage scriptLanguage, String userScript,
        ScriptComponentHistoryDataItem dataItem) throws ComponentException;

    @Override
    public void prepareOutputForRun() {
        final int buffer = 1024;
        StringWriter out = new StringWriter(buffer);
        StringWriter err = new StringWriter(buffer);

        stdoutWriter = new WorkflowConsoleForwardingWriter(out, componentContext.getLog(), ConsoleRow.Type.TOOL_OUT, null);
        stderrWriter = new WorkflowConsoleForwardingWriter(err, componentContext.getLog(), ConsoleRow.Type.TOOL_ERROR, null);
        scriptEngine.getContext().setWriter(stdoutWriter);
        scriptEngine.getContext().setErrorWriter(stderrWriter);
    }

    @Override
    public abstract void runScript() throws ComponentException;
    
    @Override
    public abstract void cancelScript();

    public long getCurrentRunNumber() {
        return componentContext.getExecutionCount();
    }

    @Override
    public abstract boolean postRun() throws ComponentException;

    protected void closeConsoleWriters() throws IOException {
        stderrWriter.flush();
        stderrWriter.close();
        stdoutWriter.flush();
        stdoutWriter.close();
    }

    //No longer needed since "dev_scripting_output" branch has been merged
//    protected TypedDatum getTypedDatum(Object value) {
//        return ScriptDataTypeHelper.getTypedDatum(value, componentContext.getService(TypedDatumService.class).getFactory());
//    }

    @Override
    public void deleteTempFiles() {
        // delete all temporary created files
        try {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempDir);
        } catch (IOException e) {
            LOGGER.error("Failed to delete temporary directory "
                + "(probably because a file were not properly closed in the Python script)", e);
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

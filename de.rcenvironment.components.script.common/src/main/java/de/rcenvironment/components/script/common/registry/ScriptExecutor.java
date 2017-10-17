/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.common.registry;

import java.io.Writer;
import java.util.Map;

import javax.script.ScriptEngine;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Interface to provide a scripting language to be executed.
 * 
 * @author Sascha Zur
 * @author Tobias Rodehutskors (cancel)
 */
public interface ScriptExecutor {

    /**
     * Prepares the executor when the component is started.
     * 
     * @param componentContext current {@link ComponentContext}
     * @return true, if preparing was successful.
     * @throws ComponentException if preparing was not successful
     */
    boolean prepareExecutor(ComponentContext componentContext) throws ComponentException;

    /**
     * This method is called each time before the runScript method. It is for preparing the next
     * run, i.e. copying files from the DataManagement.
     * 
     * @param scriptLanguage : the chosen scripting language
     * @param userScript : the script to execute
     * @param historyDataItem {@link ComponentHistoryDataItem} of the script component
     * @throws ComponentException if run fails
     */
    void prepareNewRun(ScriptLanguage scriptLanguage, String userScript,
        ScriptComponentHistoryDataItem historyDataItem) throws ComponentException;

    /**
     * Runs the script.
     * 
     * @throws ComponentException if a run fails
     */
    void runScript() throws ComponentException;
    
    /**
     * @return True, if the execution of the script can be canceled with a call of {@link ScriptExecutor#cancelScript()}; False otherwise.
     */
    boolean isCancelable();
    
    /**
     * Cancels the execution of the script.
     */
    void cancelScript();

    /**
     * Method called after runScript.
     * 
     * @return true, if component is able to run another time, else false.
     * @throws ComponentException if outcput could not be read
     */
    boolean postRun() throws ComponentException;

    /**
     * Prepares the streams for STDOUT and STDERR for the given ScriptEngine.
     */
    void prepareOutputForRun();

    /**
     * Deletes all temp files after the run.
     */
    void deleteTempFiles();

    /**
     * Reset method for nested loops.
     */
    void reset();

    /**
     * @param componentContext .
     */
    void setComponentContext(ComponentContext componentContext);

    /**
     * 
     * @param scriptEngine .
     */
    void setScriptEngine(ScriptEngine scriptEngine);

    /**
     * @param historyDataItem .
     */
    void setHistoryDataItem(ScriptComponentHistoryDataItem historyDataItem);

    /**
     * 
     * @param stateMap .
     */
    void setStateMap(Map<String, Object> stateMap);

    /**
     * @param stdoutWriter .
     */
    void setStdoutWriter(Writer stdoutWriter);

    /**
     * 
     * @param stderrWriter .
     */
    void setStderrWriter(Writer stderrWriter);

    /**
     * 
     * @param path .
     */
    void setWorkingPath(String path);

}

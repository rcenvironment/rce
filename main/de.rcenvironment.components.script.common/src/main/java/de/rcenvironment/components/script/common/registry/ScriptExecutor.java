/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.common.registry;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Interface to provide a scripting language to be executed.
 * 
 * @author Sascha Zur
 */
public interface ScriptExecutor {

    /**
     * Prepares the executor when the component is started.
     * 
     * @param componentContext current {@link ComponentContext}
     * @param notificationService current DistributedNotificationService
     * @return true, if preparing was successful.
     */
    // TODO seid_do20130523: why is notification service "injected" here and not via OSGi DS
    boolean prepareExecutor(ComponentContext componentContext, DistributedNotificationService notificationService);

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
}

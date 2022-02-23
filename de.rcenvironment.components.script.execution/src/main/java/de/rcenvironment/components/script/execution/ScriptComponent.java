/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.script.execution;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceException;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactoryRegistry;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Component to execute all kind of script languages.
 * 
 * @author Sascha Zur
 */
public class ScriptComponent extends DefaultComponent {

    private ScriptExecutorFactoryRegistry scriptExecutorRegistry;

    private ComponentContext componentContext;

    private ScriptExecutor executor;

    private String script;

    private ScriptLanguage scriptLanguage;

    private ScriptComponentHistoryDataItem historyDataItem;

    private String scriptFileRef;

    private Log log = LogFactory.getLog(ScriptComponent.class);

    private volatile boolean canceled;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getInputs().isEmpty();
    }

    @Override
    public void start() throws ComponentException {

        canceled = false;
        try {
            scriptExecutorRegistry = componentContext.getService(ScriptExecutorFactoryRegistry.class);
        } catch (ServiceException e) {
            throw new ComponentException(
                "ScriptExecutorFactoryRegistry was not initialized."
                    + " Please check your configuration and make sure the right python path is set.",
                e);

        }

        String language = componentContext.getConfigurationValue("scriptLanguage");
        scriptLanguage = ScriptLanguage.getByName(language);
        log.debug("ScriptLanguage: " + language);

        this.setExecutor(scriptExecutorRegistry.requestScriptExecutor(scriptLanguage));
        script = componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT);

        try {
            executor.prepareExecutor(componentContext);
        } catch (ComponentException e) {
            throw new ComponentException(
                "ScriptExecutorFactoryRegistry was not initialized."
                    + " Please check your configuration and make sure the right python path is set.",
                e);
        }
        if (treatStartAsComponentRun()) {
            processInputs();
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();
        executor.prepareNewRun(scriptLanguage, script, historyDataItem);
        if (historyDataItem != null) {
            if (scriptFileRef == null) {
                try {
                    scriptFileRef = componentContext.getService(ComponentDataManagementService.class)
                        .createTaggedReferenceFromString(componentContext, script);
                    historyDataItem.setScriptFileReference(scriptFileRef);
                } catch (IOException e) {
                    String errorMessage = "Failed to store Python script into the data management"
                        + "; it will not be available in the workflow data browser";
                    String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LogFactory.getLog(ScriptComponent.class),
                        errorMessage, e);
                    componentContext.getLog().componentError(errorMessage, e, errorId);

                }
            }
        }

        try {
            executor.runScript();
        } catch (ComponentException e) {
            // A ComponentException is thrown if the script execution failed as well as if the script execution was canceled. To distinguish
            // between both cases, we catch the exception and re-throw it, if the execution was not canceled by the user.

            if (canceled) {
                return;
            }
            throw e;
        }

        executor.postRun();
        writeFinalHistoryDataItem();
    }

    @Override
    public void onStartInterrupted(ThreadHandler executingThreadHandler) {
        cancelScriptExecution(executingThreadHandler);
    }

    @Override
    public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
        cancelScriptExecution(executingThreadHandler);
    }

    private void cancelScriptExecution(ThreadHandler executingThreadHandler) {
        canceled = true;

        if (executor == null) {
            // FIXME we need to delay the cancellation request, currently it is simple ignored in this case
            log.error("Cannot cancel the execution, as the script executor (Script Component) is not propertly prepared.");
            return;
        }

        if (executor.isCancelable()) {
            this.executor.cancelScript();
        } else {
            executingThreadHandler.interrupt();
        }
    }

    public void setExecutor(ScriptExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        writeFinalHistoryDataItem();
    }

    @Override
    public void tearDown(FinalComponentState state) {
        if (executor != null) {
            executor.tearDown();
        }
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new ScriptComponentHistoryDataItem();
        }
    }

    private void writeFinalHistoryDataItem() {
        if (historyDataItem != null
            && Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    @Override
    public void reset() throws ComponentException {
        super.reset();
        executor.reset();
    }
}

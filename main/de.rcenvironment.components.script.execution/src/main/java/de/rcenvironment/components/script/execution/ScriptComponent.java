/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.script.execution;

import java.io.IOException;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactoryRegistry;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Component to execute all kind of script languages.
 * 
 * @author Sascha Zur
 */
public class ScriptComponent extends DefaultComponent {

    private static DistributedNotificationService notificationService;

    private static ScriptExecutorFactoryRegistry scriptExecutorRegistry;

    private ComponentContext componentContext;

    private ScriptExecutor executor;

    private String script;

    private ScriptLanguage scriptLanguage;

    private ScriptComponentHistoryDataItem historyDataItem;

    private String scriptFileRef;

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

        scriptExecutorRegistry = componentContext.getService(ScriptExecutorFactoryRegistry.class);
        notificationService = componentContext.getService(DistributedNotificationService.class);

        String language = componentContext.getConfigurationValue("scriptLanguage");
        executor = scriptExecutorRegistry.requestScriptExecutor(ScriptLanguage.getByName(language));
        scriptLanguage = ScriptLanguage.getByName(language);
        script = componentContext.getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT);
        executor.prepareExecutor(componentContext, notificationService);

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
                    scriptFileRef = componentContext.getService(ComponentDataManagementService.class).
                        createTaggedReferenceFromString(componentContext, script);
                } catch (IOException e) {
                    LogFactory.getLog(ScriptComponent.class).error("Error writing script to data management: ", e);
                }
            }
            historyDataItem.setScriptFileReference(scriptFileRef);
        }
        executor.runScript();
        try {
            executor.postRun();
        } catch (ComponentException e) {
            componentContext.printConsoleLine("Could not parse script output. " + e.getMessage(), Type.STDERR);
            throw e;
        }
        executor.deleteTempFiles();
        writeFinalHistoryDataItem();
    }

    @Override
    public void tearDown(FinalComponentState state) {
        if (executor != null) {
            executor.deleteTempFiles();
        }

        if (state.equals(FinalComponentState.FAILED)) {
            writeFinalHistoryDataItem();
        }
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new ScriptComponentHistoryDataItem();
        }
    }

    private void writeFinalHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    @Override
    public void reset() throws ComponentException {
        super.reset();
        executor.reset();
    }
}

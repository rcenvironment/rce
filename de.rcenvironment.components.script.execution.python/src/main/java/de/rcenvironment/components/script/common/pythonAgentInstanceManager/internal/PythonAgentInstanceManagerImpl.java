/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;

import de.rcenvironment.components.script.common.pythonAgentInstanceManager.PythonAgentInstanceManager;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.scripting.python.PythonScriptEngine;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * Implementation of {@link PythonAgentInstanceManager}.
 * 
 * @author Adrian Stock
 * @author Alexander Weinert (reuse of existing python agents)
 *
 */
@Component
public class PythonAgentInstanceManagerImpl implements PythonAgentInstanceManager {

    private static final Log LOGGER = LogFactory.getLog(PythonScriptEngine.class);

    private Map<String, Integer> usageCounterByInstallationPath = new HashMap<>();

    private Map<String, PythonAgent> agentsByInstallationPath = new HashMap<>();

    private Map<String, ServerSocket> socketsByInstallationPath = new HashMap<>();

    private final AtomicInteger runningCount = new AtomicInteger(0);

    private CountDownLatch initializationSignal;

    @Override
    public synchronized PythonAgent getAgent(String pythonInstallationPath, ComponentContext compCtx)
        throws IOException {
        if (this.agentsByInstallationPath.containsKey(pythonInstallationPath)) {
            this.usageCounterByInstallationPath.compute(pythonInstallationPath, (ignored, x) -> x + 1);
            return this.agentsByInstallationPath.get(pythonInstallationPath);
        }

        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            this.socketsByInstallationPath.put(pythonInstallationPath, serverSocket);
        } catch (IOException e) {
            throw new IOException(
                "No socket could be opened. Therefore, the communication to python couldn't be established.", e);
        }

        try {
            initializationSignal = new CountDownLatch(1);

            LOGGER.debug("Starting new Python agent");

            final ServerSocket socket = this.socketsByInstallationPath.get(pythonInstallationPath);
            final PythonAgent agent = new PythonAgent(this, pythonInstallationPath, runningCount.getAndIncrement(),
                socket, initializationSignal, compCtx);
            ConcurrencyUtils.getAsyncTaskService().execute("Run Python Agent", agent);

            initializationSignal.await();
            if (!agent.wasInitializationSuccessful()) {
                throw new IOException("Unable to create a PythonAgent for the script execution.");
            }

            agentsByInstallationPath.put(pythonInstallationPath, agent);
            usageCounterByInstallationPath.put(pythonInstallationPath, 1);
            return agent;
        } catch (ScriptException | InterruptedException e) {
            throw new IOException("Unable to create a PythonAgent for the script execution.");
        }
    }

    @Override
    public synchronized boolean stopAgent(PythonAgent agent) {
        final int newUsageCount = usageCounterByInstallationPath.compute(agent.getInstallationPath(),
            (ignored, x) -> x - 1);

        if (newUsageCount == 0) {
            agent.stopInstance();
            try {
                socketsByInstallationPath.get(agent.getInstallationPath()).close();
            } catch (IOException e) {
                LOGGER.warn("Could not close server socket used for communication with python installation "
                    + agent.getInstallationPath()
                    + ". Since that agent is not used anymore, this should not implact further operation.");
            }
            usageCounterByInstallationPath.remove(agent.getInstallationPath());
            agentsByInstallationPath.remove(agent.getInstallationPath());
            return true;
        }
        return false;
    }

    @Override
    public LocalApacheCommandLineExecutor createNewExecutor() {
        try {
            return new LocalApacheCommandLineExecutor(null);
        } catch (IOException e) {
            LOGGER.error("Failed to create executor for python.");
            return null;
        }
    }
}

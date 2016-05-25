/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;
import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;
import de.rcenvironment.core.utils.cluster.ClusterJobSourceService;
import de.rcenvironment.core.utils.cluster.ClusterJobStateChangeListener;
import de.rcenvironment.core.utils.cluster.ClusterService;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchCommandLineExecutor;

/**
 * Abstract implementation of {@link ClusterService} with common functionality.
 * @author Doreen Seider
 */
public abstract class AbstractClusterService implements ClusterService {

    protected static final String REMOTE_WORK_DIR = "~";

    protected static ClusterJobSourceService informationService
        = ServiceUtils.createFailingServiceProxy(ClusterJobSourceService.class);
    
    protected SshSessionConfiguration sshConfiguration;
    
    protected Map<String, String> pathsToQueuingSystemCommands;
    
    protected Session jschSession;
    
    protected volatile long latestFetch = 0;
    
    protected Map<String, ClusterJobInformation> latestFetchedJobInformation;
    
    protected Map<String, ClusterJobState> lastClusterJobStates = new HashMap<String, ClusterJobInformation.ClusterJobState>();
    
    protected final Map<String, ClusterJobStateChangeListener> listeners = new HashMap<String, ClusterJobStateChangeListener>();

    protected Timer fetchInformationTimer;
    
    public AbstractClusterService() {}
    
    public AbstractClusterService(SshSessionConfiguration sshConfiguration, Map<String, String> pathsToQueuingSystemCommands) {
        this.sshConfiguration = sshConfiguration;
        this.pathsToQueuingSystemCommands = pathsToQueuingSystemCommands;
    }
    
    /**
     * OSGi bind method.
     * @param newService instance of {@link ClusterJobSourceService}
     */
    public void bindClusterJobSourceService(ClusterJobSourceService newService) {
        informationService = newService;
    }
    
    protected void unbindDistributedClusterJobSourceInformationService(ClusterJobSourceService oldService) {}
    
    public void setPathsToQueuingSystemCommands(Map<String, String> pathsToQueuingSystemCommands) {
        this.pathsToQueuingSystemCommands = pathsToQueuingSystemCommands;
    }
    
    @Override
    public Set<ClusterJobInformation> fetchClusterJobInformation() throws IOException {
        synchronized (this) {
            if (jschSession == null || !jschSession.isConnected()) {
                try {
                    jschSession = JschSessionFactory.setupSession(sshConfiguration.getDestinationHost(),
                        sshConfiguration.getPort(), sshConfiguration.getSshAuthUser(), null,
                        sshConfiguration.getSshAuthPhrase(), null);
                    
                } catch (JSchException e) {
                    throw new IOException("Establishing connection to cluster failed", e);
                } catch (SshParameterException e) {
                    throw new IOException("Establishing connection to cluster failed", e);
                }
            }   
        }
        
        return fetchAndParseClusterJobInformation();
    }
    
    protected abstract Set<ClusterJobInformation> fetchAndParseClusterJobInformation() throws IOException;
    
    protected String buildMainCommand(String command) {
        // with Java 8 this can be improved by Map.getOrDefault()
        if (pathsToQueuingSystemCommands.get(command) != null) {
            command = pathsToQueuingSystemCommands.get(command) + command;
        }
        return command;
    }
    
    @Override
    public void addClusterJobStateChangeListener(String jobId, final ClusterJobStateChangeListener listener) {
        synchronized (listeners) {
            listeners.put(jobId, listener);
            if (fetchInformationTimer == null) {
                fetchInformationTimer = new Timer("Fetch Cluster Job Information Timer", true);
                TimerTask fetchInformationTimerTask = new TimerTask() {
                    
                    @Override
                    public void run() {
                        final int oneSecond = 1000;
                        if (latestFetchedJobInformation == null || new Date().getTime() - latestFetch > FETCH_INTERVAL + oneSecond) {
                            try {
                                fetchClusterJobInformation();
                            } catch (IOException e) {
                                fetchInformationTimer.cancel();
                                fetchInformationTimer = null;
                                jschSession = null;
                                notifyClusterJobStateChangeListenerAboutFetchingFailure();
                                throw new RuntimeException("Fetching cluster job information failed", e);
                            }
                        }
                        notifyClusterJobStateChangeListener();
                    }
                };
                fetchInformationTimer.schedule(fetchInformationTimerTask, ClusterService.FETCH_INTERVAL,
                    ClusterService.FETCH_INTERVAL);                
            }
        }
    }
    
    private void notifyClusterJobStateChangeListener() {

        Set<String> listenersToRemove = new HashSet<String>();
        
        synchronized (listeners) {
            for (String jobId : listeners.keySet()) {
                ClusterJobInformation.ClusterJobState lastState = lastClusterJobStates.get(jobId);
                if (latestFetchedJobInformation.containsKey(jobId)) {
                    ClusterJobInformation.ClusterJobState latestState = latestFetchedJobInformation.get(jobId).getJobState();
                    if (lastState == null || !lastState.equals(latestState)) {
                        if (!listeners.get(jobId).onClusterJobStateChanged(latestState)) {
                            listenersToRemove.add(jobId);
                        }
                    }
                    lastState = latestState;
                } else {
                    if (!listeners.get(jobId).onClusterJobStateChanged(ClusterJobInformation.ClusterJobState.Unknown)) {
                        listenersToRemove.add(jobId);
                    }
                }
            }
            
            for (String jobId : listenersToRemove) {
                listeners.remove(jobId);
            }
            
            if (listeners.isEmpty()) {
                fetchInformationTimer.cancel();
                fetchInformationTimer = null;
            }
        }
    }
    
    private void notifyClusterJobStateChangeListenerAboutFetchingFailure() {
        
        synchronized (listeners) {
            for (ClusterJobStateChangeListener listener : listeners.values()) {
                listener.onClusterJobStateChanged((ClusterJobState) null);
            }
        }
    }
    
    protected String executesCommand(Session ajschSession, String command, String remoteWorkDir)
        throws IOException {
        JSchCommandLineExecutor commandLineExecutor = new JSchCommandLineExecutor(ajschSession, remoteWorkDir);
        commandLineExecutor.start(command);
        try (InputStream stdoutStream = commandLineExecutor.getStdout(); InputStream stderrStream = commandLineExecutor.getStderr();) {
            try {
                commandLineExecutor.waitForTermination();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
            String stderr = IOUtils.toString(stderrStream);
            if (stderr != null && !stderr.isEmpty()) {
                throw new IOException(stderr);
            }
            String stdout = IOUtils.toString(stdoutStream);
            return stdout;
        }
    }

}

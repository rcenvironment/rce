/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * Base class for {@link VirtualInstance} that provides management of the instance life cycle and the configured test properties.
 * 
 * @author Robert Mischke
 */
public abstract class VirtualInstanceSkeleton implements CommonVirtualInstanceControl {

    /**
     * Internal state machine controlling the instance life cycle. Triggers asynchronous execution of startup/shutdown methods.
     * 
     * @author Robert Mischke
     */
    private class StateMachine {

        private VirtualInstanceState currentState = VirtualInstanceState.INITIAL;

        private Semaphore transitionalStateSemaphore = new Semaphore(1);

        public synchronized void onTargetStateRequested(VirtualInstanceState requestedState) throws InterruptedException {
            switch (requestedState) {
            case STARTED:
                // TODO allow restart from "stopped"?
                if (currentState == VirtualInstanceState.INITIAL
                    || currentState == VirtualInstanceState.STOPPED) {
                    enterState(VirtualInstanceState.STARTING);
                    sharedThreadPool.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                performStartup();
                                enterState(VirtualInstanceState.STARTED);
                            } catch (InterruptedException e) {
                                log.debug("Virtual instance startup failed", e);
                            } catch (CommunicationException e) {
                                log.debug("Virtual instance startup failed", e);
                            }
                        }

                    });
                } else {
                    log.warn("Virtual instance was instructed to 'start', but is in " + currentState + " state");
                }
                break;

            case SIMULATED_CRASHING:
                if (currentState == VirtualInstanceState.STARTED) {
                    enterState(VirtualInstanceState.SIMULATED_CRASHING);
                    sharedThreadPool.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                performSimulatedCrash();
                                enterState(VirtualInstanceState.STOPPED);
                            } catch (InterruptedException e) {
                                log.debug("Error while simulating virtual instance crash", e);
                            }
                        }

                    });
                } else {
                    log.warn("Virtual instance was commanded to 'crash', but in " + currentState + " state (instead of STARTED)");
                }
                break;

            case STOPPED:
                // TODO react on "stop" requests during startup?
                if (currentState == VirtualInstanceState.STARTED) {
                    enterState(VirtualInstanceState.STOPPING);
                    sharedThreadPool.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                performShutdown();
                                enterState(VirtualInstanceState.STOPPED);
                            } catch (InterruptedException e) {
                                log.debug("Virtual instance shutdown failed", e);
                            }
                        }

                    });
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid state request: " + requestedState);
            }
        }

        public synchronized VirtualInstanceState getCurrentState() {
            return currentState;
        }

        // Note: this method MUST NOT be synchronized; otherwise, the VI monitor will be held
        // during the wait and prevent the expected state change to happen -- misc_ro
        public void waitForNonTransitionalState() throws InterruptedException {
            // wait until the semaphore is not held
            transitionalStateSemaphore.acquire();
            // release immediately
            transitionalStateSemaphore.release();
        }

        // Note: synchronized for access after asynchronous state changes
        private synchronized void enterState(VirtualInstanceState newState) throws InterruptedException {
            if (isTransitionalState(currentState)) {
                transitionalStateSemaphore.release();
            }
            if (isTransitionalState(newState)) {
                transitionalStateSemaphore.acquire();
            }
            currentState = newState;
            // log.debug(StringUtils.format("State change: Virtual instance '%s' is now %s",
            // nodeInformation.getLogName(), newState));
        }

        private boolean isTransitionalState(VirtualInstanceState state) {
            // TODO make this an Enum property?
            return state == VirtualInstanceState.STARTING || state == VirtualInstanceState.STOPPING
                || state == VirtualInstanceState.SIMULATED_CRASHING;
        }

    }

    private static final ThreadPool sharedThreadPool = SharedThreadPool.getInstance();

    protected final InitialNodeInformation nodeInformation;

    protected final Log log = LogFactory.getLog(getClass());

    private final NodeConfigurationServiceTestStub nodeConfigurationService;

    private final StateMachine stateMachine = new StateMachine();

    /**
     * Creates a virtual instance with the given log/display name.
     * 
     * @param logName the log/display name to use
     * @param isRelay whether the "is relay" flag of this node should be set
     */
    public VirtualInstanceSkeleton(String nodeId, String logName, boolean isRelay) {
        nodeConfigurationService = new NodeConfigurationServiceTestStub(nodeId, logName, isRelay);
        nodeInformation = nodeConfigurationService.getInitialNodeInformation();
    }

    /**
     * Adds a {@link NetworkContactPoint} at which a network server should be run by this instance.
     * 
     * @param contactPoint the {@link NetworkContactPoint} that defines the server bind address, port and transport type
     */
    public void addServerConfigurationEntry(NetworkContactPoint contactPoint) {
        nodeConfigurationService.addServerConfigurationEntry(contactPoint);
    }

    @Override
    public void addInitialNetworkPeer(NetworkContactPoint contactPoint) {
        nodeConfigurationService.addInitialNetworkPeer(contactPoint);
    }

    public VirtualInstanceState getCurrentState() {
        return stateMachine.getCurrentState();
    }

    @Override
    public void setTargetState(VirtualInstanceState requestedState) throws InterruptedException {
        stateMachine.onTargetStateRequested(requestedState);
    }

    @Override
    public void waitForStateChangesToFinish() throws InterruptedException {
        stateMachine.waitForNonTransitionalState();
    }

    @Override
    public void start() throws InterruptedException {
        setTargetState(VirtualInstanceState.STARTED);
        waitForStateChangesToFinish();
    }

    @Override
    public void simulateCrash() throws InterruptedException {
        setTargetState(VirtualInstanceState.SIMULATED_CRASHING);
        waitForStateChangesToFinish();
    }

    @Override
    public void shutDown() throws InterruptedException {
        setTargetState(VirtualInstanceState.STOPPED);
        waitForStateChangesToFinish();
    }

    @Override
    public abstract void registerNetworkTransportProvider(NetworkTransportProvider provider);

    protected NodeConfigurationService getNodeConfigurationService() {
        return nodeConfigurationService;
    }

    protected abstract void performStartup() throws InterruptedException, CommunicationException;

    protected abstract void performShutdown() throws InterruptedException;

    protected abstract void performSimulatedCrash() throws InterruptedException;

}

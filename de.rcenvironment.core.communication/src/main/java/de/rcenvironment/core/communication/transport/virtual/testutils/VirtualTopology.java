/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.virtual.testutils;

import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.VirtualInstanceGroup;

/**
 * Utility wrapper to set up and handle test topologies. (Note: This is a redesigned successor of the {@link VirtualInstanceTestUtils}
 * class.)
 * 
 * TODO move to de.rcenvironment.core.communication.testutils
 * 
 * @author Robert Mischke
 */
public class VirtualTopology {

    private VirtualInstance[] instances;

    private VirtualInstanceGroup allInstancesGroup;

    private VirtualInstanceTestUtils testUtils = new VirtualInstanceTestUtils(null, null);

    private int lastIndex;

    private boolean connectBothDirectionsByDefaultFlag;

    public VirtualTopology(VirtualInstance... instances) {
        this.instances = instances;
        allInstancesGroup = new VirtualInstanceGroup(instances);
        lastIndex = instances.length - 1;
    }

    public int getInstanceCount() {
        return instances.length;
    }

    public VirtualInstance[] getInstances() {
        return instances;
    }

    /**
     * Returns the instance with the given zero-based index.
     * 
     * @param i the instance index
     * @return the instance
     */
    public VirtualInstance getInstance(int i) {
        return instances[i];
    }

    public VirtualInstance getFirstInstance() {
        return instances[0];
    }

    public VirtualInstance getLastInstance() {
        return instances[lastIndex];
    }

    public VirtualInstance getRandomInstance() {
        return testUtils.getRandomInstance(instances);
    }

    /**
     * Returns a random instance, but not one of the given ones.
     * 
     * @param not the instances to exclude from the candidates
     * @return the randomly-chosen instance
     */
    public VirtualInstance getRandomInstanceExcept(VirtualInstance... not) {
        return testUtils.getRandomInstance(instances, not);
    }

    public VirtualInstanceGroup getAsGroup() {
        return allInstancesGroup;
    }

    /**
     * Connects the instances given by their zero-based indices. Depending on the flag set by
     * {@link #setConnectBothDirectionsByDefaultFlag(boolean)}, the connection is initiated in one or both directions. The default behavior
     * is to only connect the from->to direction.
     * 
     * @param from the connecting instance
     * @param to the connected-to instance
     */
    public void connect(int from, int to) {
        connect(from, to, connectBothDirectionsByDefaultFlag);

    }

    /**
     * Connects the instances given by their zero-based indices. Depending on the boolean parameter, the connection is initiated in one or
     * both directions.
     * 
     * @param from the connecting instance
     * @param to the connected-to instance
     * @param bothDirections true if the connection should be initiated in both directions; if false, it is only initiated in the from->to
     *        direction
     */
    public void connect(int from, int to, boolean bothDirections) {
        NetworkContactPoint targetSCP = instances[to].getDefaultContactPoint();
        instances[from].connectAsync(targetSCP);
        if (bothDirections) {
            connect(to, from, false); // explicit "false" is required here; do not remove
        }
    }

    /**
     * Blocks until the second node is part of the first node's reachable network topology, or until the timeout is reached.
     * 
     * Note that for this explicit method, there is no special handling in case of bidirectional connections (see
     * {@link #setConnectBothDirectionsByDefaultFlag(boolean)}).
     * 
     * @param from the instance that should "see" the second instance
     * @param to the instance that should be "seen" by the first instance
     * @param timeout the maximum time to wait
     * @throws InterruptedException on interruption
     * @throws TimeoutException on timeout
     */
    public void waitUntilReachable(int from, int to, int timeout) throws InterruptedException, TimeoutException {
        getInstance(from).waitUntilContainsInReachableNodes(getInstance(to).getInstanceNodeSessionId(), timeout);
    }

    /**
     * Connects the instances given by their zero-based indices, and waits until the second node is part of the first node's reachable
     * network topology, or until the timeout is reached. If the flag set by {@link #setConnectBothDirectionsByDefaultFlag(boolean)} is
     * true, the connection is initiated in both directions, and both directions are waited for.
     * 
     * @param from the connecting instance
     * @param to the connected-to instance
     * @param timeoutMsec the maximum time to wait
     * @throws TimeoutException if the wait time is exceeded
     * @throws InterruptedException on thread interruption
     */
    public void connectAndWait(int from, int to, int timeoutMsec) throws TimeoutException, InterruptedException {
        connect(from, to);
        waitUntilReachable(from, to, timeoutMsec);
        if (connectBothDirectionsByDefaultFlag) {
            waitUntilReachable(to, from, timeoutMsec);
        }
    }

    /**
     * Connects all instances to a sequentially-connected "chain".
     * 
     * @param bothDirections see {@link #connect(int, int, boolean)}
     */
    public void connectToChain(boolean bothDirections) {
        connectToChain(0, lastIndex, bothDirections);
    }

    /**
     * Connects a range of instances to a sequentially-connected "chain".
     * 
     * @param from the start of the range
     * @param to the end of the range
     * @param bothDirections see {@link #connect(int, int, boolean)}
     */
    public void connectToChain(int from, int to, boolean bothDirections) {
        if (to < from) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < to; i++) {
            connect(i, i + 1, bothDirections);
        }
    }

    /**
     * Connects all instances to a closed ring/loop.
     * 
     * @param bothDirections see {@link #connect(int, int, boolean)}
     */
    public void connectToRing(boolean bothDirections) {
        connectToRing(0, lastIndex, bothDirections);
    }

    /**
     * Connects a range of instances to a closed ring/loop.
     * 
     * @param from the start of the range
     * @param to the end of the range
     * @param bothDirections see {@link #connect(int, int, boolean)}
     */
    public void connectToRing(int from, int to, boolean bothDirections) {
        // note: "from <= to" check is performed in connectToChain()
        connectToChain(from, to, bothDirections);
        // close loop
        connect(to, from, bothDirections);
    }

    /**
     * Determines whether all instances consider themselves "converged".
     * 
     * TODO add definition of convergence
     * 
     * @return true if all instances consider themselves "converged"
     */
    public boolean allInstancesConverged() {
        return testUtils.allInstancesHaveSameRawNetworkGraph(instances);
    }

    public void setConnectBothDirectionsByDefaultFlag(boolean connectBothDirectionsByDefaultFlag) {
        this.connectBothDirectionsByDefaultFlag = connectBothDirectionsByDefaultFlag;
    }

}

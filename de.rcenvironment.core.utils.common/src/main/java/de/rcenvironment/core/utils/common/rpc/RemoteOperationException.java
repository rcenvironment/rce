/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.rpc;

/**
 * An exception class representing the failure of a remote operation, typically a remote procedure call (RPC).
 * <p>
 * In case of RPCs, this exception covers both failures on the receiving node (especially uncaught {@link RuntimeException}s from service
 * methods), as well as network errors that may occur on the path between the caller and that node. For example, if the destination node is
 * not reachable at the moment of the call, this exception may be thrown without a network message ever leaving the local node.
 * <p>
 * Note that is it also possible for this exception to be thrown after the remote operation was performed without errors, but an error
 * occurred when transmitting the operation's result back across the network. Because of this, client code must take care to not make
 * unfounded assumptions about the state of the remote node after such an exception.
 * 
 * @author Robert Mischke
 */
public final class RemoteOperationException extends Exception {

    private static final long serialVersionUID = -8379457474465190182L;

    public RemoteOperationException(String message) {
        super(message);
    }
}

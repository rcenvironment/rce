/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Keeps track of pending requests, and associates incoming responses for them. For each request, a timeout is specified, after which the
 * generated {@link Future} is completed with an empty {@link Optional} result.
 *
 * @param <TKey> the association key type
 * @param <TResponse> the response type
 * @author Robert Mischke
 */
public interface BlockingResponseMapper<TKey, TResponse> {

    /**
     * Registers a request key that was typically sent out along with a network request.
     * 
     * @param key the association key
     * @param timeoutMsec the timeout to abort waiting for a response
     * @return the response, or an empty {@link Optional} on timeout
     */
    Future<Optional<TResponse>> registerRequest(TKey key, int timeoutMsec);

    /**
     * Reports a received response with the provided association key, typically sent back by the remote resource.
     * 
     * @param key the association key
     * @param response the response object
     */
    void registerResponse(TKey key, TResponse response);
}

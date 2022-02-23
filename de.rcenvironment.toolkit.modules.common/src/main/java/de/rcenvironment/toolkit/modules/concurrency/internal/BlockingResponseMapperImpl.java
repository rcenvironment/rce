/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.BlockingResponseMapper;

/**
 * Default {@link BlockingResponseMapper} implementation.
 *
 * @param <TKey> the association key type
 * @param <TResponse> the response type
 * @author Robert Mischke
 */
public class BlockingResponseMapperImpl<TKey, TResponse> implements BlockingResponseMapper<TKey, TResponse> {

    private final Map<TKey, CompletableFuture<Optional<TResponse>>> requestFutures = new HashMap<>();

    private final AsyncTaskService asyncTaskService;

    private final Log log = LogFactory.getLog(getClass());

    public BlockingResponseMapperImpl(AsyncTaskService asyncTaskService) {
        this.asyncTaskService = asyncTaskService;
    }

    @Override
    public Future<Optional<TResponse>> registerRequest(TKey key, int timeoutMsec) {
        final CompletableFuture<Optional<TResponse>> future = new CompletableFuture<>();
        synchronized (requestFutures) {
            requestFutures.put(key, future);
        }
        asyncTaskService.scheduleAfterDelay("BlockingResponseMapper: Check for response timeouts", () -> checkForTimeout(key), timeoutMsec);
        return future;
    }

    @Override
    public void registerResponse(TKey key, TResponse response) {
        final CompletableFuture<Optional<TResponse>> pendingFuture;
        synchronized (requestFutures) {
            pendingFuture = requestFutures.remove(key);
        }
        if (pendingFuture != null) {
            pendingFuture.complete(Optional.of(response));
        } else {
            log.debug("Received a response for request key " + key + ", but the timeout was already reached");
        }
    }

    private void checkForTimeout(TKey key) {
        final CompletableFuture<Optional<TResponse>> pendingFuture;
        synchronized (requestFutures) {
            pendingFuture = requestFutures.remove(key);
        }
        if (pendingFuture != null) {
            log.debug("Reached response timeout for request key " + key);
            pendingFuture.complete(Optional.empty());
        }
    }

}

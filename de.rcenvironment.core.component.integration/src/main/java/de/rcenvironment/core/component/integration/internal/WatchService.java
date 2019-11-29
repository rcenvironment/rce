/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;

import org.osgi.service.component.annotations.Component;

/**
 * A wrapper that mainly delegates to {@link java.nio.file.WatchService}. This wrapper is mainly used in order to make it easier to inject a
 * dependency on a watch service into other classes.
 * 
 * @author Alexander Weinert
 */
public class WatchService {

    private final java.nio.file.WatchService delegate;

    /**
     * Builder class for WatchService. This class must be used to construct a WatchService, as the latter class is only a delegate to
     * java.nio.file.WatchService. Thus, if using this class directly as an OSGI component, only a single java.nio.file.WatchService would
     * be used, i.e., closing one WatchService would close all of them. Hence, we use this builder class that allows for the creation of
     * multiple WatchServices.
     *
     * @author Alexander Weinert
     */
    @Component(service = WatchService.Builder.class)
    public static class Builder {

        /**
         * @return A new instance of WatchService
         * @throws IOException If acquiring the delegate via FileSystems.getDefault().newWatchService() throws an IOException.
         */
        public WatchService build() throws IOException {
            return new WatchService(FileSystems.getDefault().newWatchService());
        }
    }

    WatchService(java.nio.file.WatchService delegateParam) {
        delegate = delegateParam;
    }

    /**
     * Wraps dir.register(WatchService, events).
     * 
     * @param dir    The directory to be watched.
     * @param events The events to be watched for.
     * @return A watch key representing the registration of a watcher with the given directory
     * @throws IOException If the registration of the directory with the watcher throws an IOException.
     */
    public WatchKey watch(Path dir, Kind<?>... events) throws IOException {
        return dir.register(delegate, events);
    }

    /**
     * Wraps {@link java.nio.file.WatchService}.close().
     * 
     * @throws IOException If {@link java.nio.file.WatchService}.close() throws an IOException.
     */
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * 
     * Wraps {@link java.nio.file.WatchService}.take().
     * 
     * @return A watch key representing the observation of the watched directory.
     * @throws InterruptedException If the waiting for an event is interrupted.
     */
    public WatchKey take() throws InterruptedException {
        return delegate.take();
    }

}

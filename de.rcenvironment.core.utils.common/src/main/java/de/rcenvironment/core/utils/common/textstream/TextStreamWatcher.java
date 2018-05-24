/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A utility class to read and forward line-based text data from an {@link InputStream}. As it requires an {@link AsyncTaskService} to run,
 * an instance must be provided via the constructor. If available in the project scope, consider using
 * de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory for convenience.
 * 
 * @author Robert Mischke
 */
public class TextStreamWatcher {

    private final AsyncTaskService asyncTaskService;

    private final List<WatcherRunnable> watcherRunnables = new ArrayList<>(4);

    private List<Future<?>> watcherTaskFutures; // list created on start; also serves as "started" marker

    /**
     * The actual {@link Runnable} that performs the output capture. Uses the outer class fields for delegation.
     * 
     * @author Robert Mischke
     */
    private final class WatcherRunnable implements Runnable {

        private BufferedReader bufferedReader;

        private final TextOutputReceiver[] receivers;

        // private volatile boolean terminated = false;

        /**
         * Constructs a new {@link Runnable} to watch the given {@link InputStream}, and forward each line to all receivers.
         * 
         * @param inputStream the stream to watch
         * @param receivers the receivers of line output and life cycle events
         */
        private WatcherRunnable(InputStream inputStream, TextOutputReceiver... receivers) {

            // guard against null parameters
            Objects.requireNonNull(inputStream, "The input stream to be read from cannot be null");
            for (TextOutputReceiver r : receivers) {
                Objects.requireNonNull(r, "Received a 'null' receiver argument");
            }

            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            this.receivers = receivers;
        }

        @Override
        @TaskDescription("Text stream watching/reading")
        public void run() {
            for (TextOutputReceiver receiver : receivers) {
                receiver.onStart();
            }
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    for (TextOutputReceiver receiver : receivers) {
                        receiver.addOutput(line);
                    }
                }
                for (TextOutputReceiver receiver : receivers) {
                    receiver.onFinished();
                }
            } catch (IOException e) {
                for (TextOutputReceiver receiver : receivers) {
                    try {
                        receiver.onFatalError(e);
                    } catch (RuntimeException e1) {
                        // catch this to make sure the other "onException" handlers are called
                        LogFactory.getLog(getClass()).error("Exception in onException() callback", e1);
                    }
                }
            }

            IOUtils.closeQuietly(bufferedReader);
            // terminated = true;
        }
    }

    /**
     * Creates an instance (which is not started automatically; see {@link #start()}). Typically, stream mappings are added before starting
     * it.
     * 
     * @param asyncTaskService the {@link AsyncTaskService} instance to submit tasks to
     */
    public TextStreamWatcher(AsyncTaskService asyncTaskService) {
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * Backwards compatibility / convenience constructor; constructs an instance with a predefined stream mapping.
     *
     * TODO change order of parameters
     * 
     * @param input the {@link InputStream} to read from
     * @param asyncTaskService the {@link AsyncTaskService} instance to submit tasks to
     * @param receivers the {@link TextOutputReceiver} to send the generated events to
     */
    public TextStreamWatcher(InputStream input, AsyncTaskService asyncTaskService, TextOutputReceiver... receivers) {
        this.asyncTaskService = asyncTaskService;
        registerStream(input, receivers);
    }

    /**
     * Registers an {@link InputStream} and the {@link TextOutputReceiver}s that this stream's lines should be sent to.
     * 
     * @param inputStream the stream to read from
     * @param receivers the receivers to send text lines and life cycle events to
     * 
     * @return the "self" instance for call chaining
     */
    public synchronized TextStreamWatcher registerStream(InputStream inputStream, TextOutputReceiver... receivers) {
        watcherRunnables.add(new WatcherRunnable(inputStream, receivers));
        return this;
    }

    /**
     * Starts the watcher thread.
     * 
     * @return the "self" instance for call chaining
     */
    public synchronized TextStreamWatcher start() {
        if (wasStarted()) {
            throw new IllegalStateException("Watcher task was already started");
        }
        watcherTaskFutures = new ArrayList<Future<?>>(watcherRunnables.size());
        for (WatcherRunnable watcherRunnable : watcherRunnables) {
            watcherTaskFutures.add(this.asyncTaskService.submit(watcherRunnable));
        }
        return this;
    }

    /**
     * Blocks until the stream watcher thread has terminated.
     */
    public void waitForTermination() {

        final List<Future<?>> watcherTaskFuturesCopy; // immutable copy to release the synchronization lock to allow for cancel() calls
        synchronized (this) {
            if (!wasStarted()) {
                throw new IllegalStateException("Watcher task was not started yet");
            }
            watcherTaskFuturesCopy = new ArrayList<>(watcherTaskFutures);
        }
        try {
            for (Future<?> watcherTaskFuture : watcherTaskFuturesCopy) {
                watcherTaskFuture.get();
            }
        } catch (InterruptedException e) {
            LogFactory.getLog(getClass()).debug("Interrupted while waiting for stream watcher task to finish");
        } catch (ExecutionException e) {
            LogFactory.getLog(getClass()).warn("Exception while waiting for stream watcher task to finish", e);
        }
    }

    /**
     * Interrupts the stream watcher thread.
     */
    public synchronized void cancel() {
        if (!wasStarted()) {
            throw new IllegalStateException("Watcher task was not started yet");
        }
        for (Future<?> watcherTaskFuture : watcherTaskFutures) {
            watcherTaskFuture.cancel(true);
        }
    }

    private boolean wasStarted() {
        return watcherTaskFutures != null;
    }

}

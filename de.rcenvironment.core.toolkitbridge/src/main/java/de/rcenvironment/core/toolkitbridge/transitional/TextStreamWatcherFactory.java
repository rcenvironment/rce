/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.toolkitbridge.transitional;

import java.io.InputStream;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * A factory that creates {@link TextStreamWatcher} instances implicitly configured with the default {@link AsyncTaskService}. May become
 * obsolete once the internal thread pool / {@link AsyncTaskService} dependency in {@link TextStreamWatcher} is eliminated.
 * 
 * @author Robert Mischke
 */
public final class TextStreamWatcherFactory {

    private TextStreamWatcherFactory() {}

    /**
     * Creates a {@link TextStreamWatcher} using the global {@link Toolkit} instance.
     * 
     * @return the new {@link TextStreamWatcher} instance
     */
    public static TextStreamWatcher createWatcher() {
        return new TextStreamWatcher(ConcurrencyUtils.getAsyncTaskService());
    }

    /**
     * Creates a {@link TextStreamWatcher} using the global {@link Toolkit} instance and a pre-registered input stream.
     * 
     * @param input see {@link TextStreamWatcher#TextStreamWatcher (InputStream, AsyncTaskService, TextOutputReceiver...) }
     * @param receivers see {@link TextStreamWatcher#TextStreamWatcher(InputStream, AsyncTaskService, TextOutputReceiver...) }
     * @return the new {@link TextStreamWatcher} instance
     */
    public static TextStreamWatcher create(InputStream input, TextOutputReceiver... receivers) {
        return new TextStreamWatcher(input, ConcurrencyUtils.getAsyncTaskService(), receivers);
    }

}

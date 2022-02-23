/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.logging;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * A custom appender class that can switch from its initial location to another location specified at runtime. In the case of RCE, this is
 * the profile folder's location, which is not known at early startup yet.
 *
 * @author Robert Mischke
 */
@Plugin(name = SelectableLocationFileAppender.PLUGIN_NAME, elementType = Appender.ELEMENT_TYPE, category = Core.CATEGORY_NAME)
public final class SelectableLocationFileAppender extends AbstractOutputStreamAppender<FileManager> {

    protected static final String PLUGIN_NAME = "SelectableLocationFileAppender";

    private static final String SYSTEM_PROPERTY_ENABLE_DEBUG_OUTPUT = "rce.debug.logging";

    // using buffered IO for performance, at the price of potential log cut-off on hard process kill/crash
    // TODO >10.3.1: setting this to false does not seem to actually disable buffering; tracked as Mantis #0017870
    private static final boolean USE_IO_BUFFERING = true;

    // the buffer size for low-level file buffering (as in standard log4j appenders);
    // set fairly low (default is 8192) as a compromise to get fairly up-to-date log flushing
    private static final int DEFAULT_IO_BUFFER_SIZE = 256;

    private static final int EARLY_LOG_EVENT_CAPTURE_BUFFER_SIZE = 100; // log events (custom early log capturing)

    // if this substring appears in a location property's value, it is considered unresolved
    private static final CharSequence UNRESOLVED_PROPERTY_MARKER = "${";

    private static final Map<String, SelectableLocationFileAppender> REGISTERED_INSTANCES = new HashMap<>();

    private static final Map<String, List<LogEvent>> EARLY_LOG_EVENT_CAPTURE_BUFFERS = new HashMap<>();

    // for debug output only
    private static final AtomicInteger sharedInstantiationCounter = new AtomicInteger();

    private static final boolean VERBOSE_DEBUG_OUTPUT = System.getProperty(SYSTEM_PROPERTY_ENABLE_DEBUG_OUTPUT) != null;

    private List<LogEvent> earlyLogEventCaptureList;

    // TODO remove logFileLocation paramter?
    protected SelectableLocationFileAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
        final FileManager fileManager, final File logFileLocation, boolean ignoreExceptions, final boolean immediateFlush,
        Property[] properties) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, properties, fileManager);

        // fetch the associated buffer for capturing early log events, if there is one
        synchronized (EARLY_LOG_EVENT_CAPTURE_BUFFERS) {
            this.earlyLogEventCaptureList = EARLY_LOG_EVENT_CAPTURE_BUFFERS.get(this.getName());
        }

        // if there is an early log capture buffer, insert all captured events into this new log file
        if (earlyLogEventCaptureList != null) {
            // this synchronization should be unnecessary as log4j should prevent multi-threaded calls, but better be sure
            synchronized (earlyLogEventCaptureList) {
                for (LogEvent event : earlyLogEventCaptureList) {
                    // invoke the super method to prevent re-capturing the inserted events; must
                    // be adapted if there are other relevant changes in the local append method
                    super.append(event);
                    if (VERBOSE_DEBUG_OUTPUT) {
                        System.err.println("LOG SYSTEM DEBUG: Injecting buffered log event into the new "
                            + this.getName() + " appender: " + event.getMessage().getFormat());
                    }
                }
            }
        }
    }

    @PluginFactory
    public static SelectableLocationFileAppender createAppender(
        @PluginAttribute("name") String internalLoggerName,
        @PluginAttribute("initialLocation") String initialLocation,
        // note: will contain unresolved placeholders in "initial" phase
        @PluginAttribute("finalLocation") String finalLocation,
        @PluginElement("Layout") Layout<?> layout,
        @PluginElement("Filter") Filter filter) {

        // extracted into variables for readability; should be eliminated by compiler
        final boolean append = false;
        final boolean locking = false;
        final boolean bufferedIo = USE_IO_BUFFERING;
        final boolean createOnDemand = false;
        final String advertiseUri = null;
        final int bufferSize = DEFAULT_IO_BUFFER_SIZE;
        final String filePermissions = null;
        final String fileOwner = null;
        final String fileGroup = null;
        Configuration configuration = null; // not sure what this is needed for; seems to be Builder.getConfiguration()

        // determine the current location that should be used, and optionally whether there is a previous one
        SimpleEntry<File, Optional<File>> result = resolveEffectiveLogFileLocation(initialLocation, finalLocation);

        // unwrap the return type
        final File effectiveLocation = result.getKey();
        final Optional<File> optionalPreviousLocation = result.getValue();

        final FileManager fileManager =
            FileManager.getFileManager(effectiveLocation.getPath(), append, locking, bufferedIo, createOnDemand,
                advertiseUri, layout, bufferSize, filePermissions, fileOwner, fileGroup, configuration);

        synchronized (EARLY_LOG_EVENT_CAPTURE_BUFFERS) {
            if (EARLY_LOG_EVENT_CAPTURE_BUFFERS.get(internalLoggerName) == null) {
                EARLY_LOG_EVENT_CAPTURE_BUFFERS.put(internalLoggerName, new ArrayList<>(EARLY_LOG_EVENT_CAPTURE_BUFFER_SIZE));
            }
        }

        SelectableLocationFileAppender newInstance =
            new SelectableLocationFileAppender(internalLoggerName, layout, filter, fileManager, effectiveLocation, true, false, null);

        // after the constructor called above will have injected the buffered log entries, first flush the new appender...
        fileManager.flush();
        // ...and then delete the previous log file if one exists (i.e., configured and not already deleted)
        if (optionalPreviousLocation.isPresent()) {
            File previousLocation = optionalPreviousLocation.get();
            // note: LOGGER output would not typically be seen, so using StdErr here
            if (previousLocation.delete()) {
                if (VERBOSE_DEBUG_OUTPUT) {
                    System.err.println("LOG SYSTEM DEBUG: Deleted initial log file " + previousLocation.toString());
                }
            } else {
                // unusual case, so just print to StdErr to get the best chance of being notices
                System.err.println("Failed to delete initial log file " + previousLocation.toString());
            }
        }

        synchronized (REGISTERED_INSTANCES) {
            // Currently, this appender class is instantiated twice for each appender configuration on startup, as the
            // whole log4j configuration is invalidated in Pax right after being applied. The reason for this is not
            // investigated yet. As the impact is fairly low, however, it does not have a high priority. For robustness
            // against such issues, and similar future ones, appenders are allowed to replace previous ones. -- misc_ro
            REGISTERED_INSTANCES.put(internalLoggerName, newInstance);
        }

        return newInstance;
    }

    private static SimpleEntry<File, Optional<File>> resolveEffectiveLogFileLocation(String initialLocation, String finalLocation) {

        boolean isInitialLocationResolved = (initialLocation != null) && !initialLocation.contains(UNRESOLVED_PROPERTY_MARKER);
        boolean isFinalLocationResolved = (finalLocation != null) && !finalLocation.contains(UNRESOLVED_PROPERTY_MARKER);

        if (!isInitialLocationResolved && !isFinalLocationResolved) {
            throw new IllegalStateException(
                "Unable to configure log file location: Neither the initial location '" + initialLocation + "' nor the final location '"
                    + finalLocation + "' is a resolved path");
        }

        final File effectiveLocation;
        Optional<File> optionalPreviousLocation = Optional.empty(); // default

        if (isFinalLocationResolved) {
            effectiveLocation = toCanonicalFileWithErrorHandling(finalLocation);
            if (isInitialLocationResolved) {
                optionalPreviousLocation = Optional.of(toCanonicalFileWithErrorHandling(initialLocation));
            }
        } else {
            effectiveLocation = toCanonicalFileWithErrorHandling(initialLocation);
        }

        if (VERBOSE_DEBUG_OUTPUT) {
            String message = "LOG SYSTEM DEBUG: Initializing custom appender #" + sharedInstantiationCounter.incrementAndGet()
                + " for log file " + effectiveLocation
                + " (initial location param: " + initialLocation + " , final location param: " + finalLocation;
            System.err.println(message);
        }

        // lending a standard Java type to return two values
        return new SimpleEntry<>(effectiveLocation, optionalPreviousLocation);
    }

    private static File toCanonicalFileWithErrorHandling(String locationString) {
        try {
            return new File(locationString).getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to convert log file location '"
                + locationString + "' to a canonical path", e);
        }
    }

    public static void configureFinalLogEnvironment(File baseDirectory, String filenamePrefix) {

        if (filenamePrefix == null) {
            filenamePrefix = "";
        }

        // TODO (p1) 10.3.1: ensure proper output (and behavior) if file logging is disabled

        LOGGER.debug("Reconfiguring log parameters: new base directory='" + baseDirectory + "', filename prefix='" + filenamePrefix + "'");

        System.setProperty("rce.profilePath", baseDirectory.getAbsolutePath());
        System.setProperty("rce.logFilenamesPrefix", filenamePrefix);
    }

    public static void finalizeLogRelocationForAllAppenders() {

        synchronized (REGISTERED_INSTANCES) {
            for (Entry<String, SelectableLocationFileAppender> instance : REGISTERED_INSTANCES.entrySet()) {
                final SelectableLocationFileAppender appenderInstance = instance.getValue();
                appenderInstance.finalizeLogRelocation();
            }
        }
        synchronized (EARLY_LOG_EVENT_CAPTURE_BUFFERS) {
            EARLY_LOG_EVENT_CAPTURE_BUFFERS.clear(); // release buffers to conserve some memory
        }
    }

    @Override
    public void append(LogEvent event) {
        // low-overhead defence against a race condition when this field is nulled
        final List<LogEvent> immutableCopyOfEarlyLogEventCaptureList = earlyLogEventCaptureList;
        if (immutableCopyOfEarlyLogEventCaptureList != null) {
            // this synchronization should be unnecessary as log4j should prevent multi-threaded calls, but better be sure
            synchronized (immutableCopyOfEarlyLogEventCaptureList) {
                immutableCopyOfEarlyLogEventCaptureList.add(event);
            }
        }

        if (VERBOSE_DEBUG_OUTPUT) {
            System.err.println("LOG SYSTEM DEBUG: Appending to " + this.getName() + "(" + System.identityHashCode(this) + "/"
                + System.identityHashCode(this.getClass()) + "): " + event.getMessage().getFormat());
        }
        super.append(event);
    }

    private void finalizeLogRelocation() {

        // Release all buffered early log events to conserve memory; this also disables buffering further ones
        earlyLogEventCaptureList = null;
    }
}

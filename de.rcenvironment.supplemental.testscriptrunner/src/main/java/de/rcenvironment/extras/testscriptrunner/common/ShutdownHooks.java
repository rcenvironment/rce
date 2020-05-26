/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple on-demand shutdown hooks; intended for context-specific actions like "shut down all remaining connections/processes".
 * 
 * @author Robert Mischke
 */
// TODO this was used in the former "standalone test runner" setup; migrate to test/scenario @after annotations -- misc_ro
public final class ShutdownHooks {

    private static final ShutdownHooks sharedInstance = new ShutdownHooks();

    private Map<String, Runnable> hooksById = new HashMap<>();

    private final Log log = LogFactory.getLog(getClass());

    private ShutdownHooks() {}

    /**
     * Registers a shutdown hook; if an id is reused, the old hook is replaced.
     * 
     * @param id the hook's unique id; can be used for replacing
     * @param runnable the hook {@link Runnable}
     */
    public static synchronized void register(String id, Runnable runnable) {
        sharedInstance.hooksById.put("id", runnable);
    }

    /**
     * Executes all previously registered hooks.
     */
    public static synchronized void execute() {
        for (Runnable hook : sharedInstance.hooksById.values()) {
            try {
                hook.run();
            } catch (RuntimeException e) {
                sharedInstance.log.error("Error while executing shutdown hook", e);
            }
        }
        sharedInstance.hooksById = null; // enforce failure in case a new hook is added afterwards
    }
}

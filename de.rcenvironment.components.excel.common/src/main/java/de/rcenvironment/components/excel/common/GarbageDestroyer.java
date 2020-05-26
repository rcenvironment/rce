/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Simple Thread-Class for calling garbage collector manually.
 *
 * @author Markus Kunde
 */
public class GarbageDestroyer implements Runnable {

    @TaskDescription("Run garbage collector.")
    @Override
    public void run() {
        System.gc();

    }

}

/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import java.util.List;

/**
 * Defines a benchmark run. Currently, the top-level configuration is just a collection of
 * {@link BenchmarkSubtask}s. When providing multiple subtasks, they are executed in parallel.
 * 
 * @author Robert Mischke
 */
public interface BenchmarkSetup {

    /**
     * @return the list of subtasks to execute in parallel
     */
    List<? extends BenchmarkSubtask> getSubtasks();
}

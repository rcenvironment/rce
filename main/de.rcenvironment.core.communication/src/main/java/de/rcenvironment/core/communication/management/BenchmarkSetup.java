/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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

/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.api;


/**
 * A concurrency mechanism to group sequentially-created elements into ordered batches. A batch is returned (to a given
 * {@link BatchProcessor}) when a certain maximum number of elements is reached, or after a specified time has elapsed since the batch was
 * created. Batches are created implicitly when an element is added while no batch is already active.
 * 
 * @param <T> the element type to aggregate
 * 
 * @author Robert Mischke
 */
public interface BatchAggregator<T> {

    /**
     * Adds an element for aggregation. May trigger the internal creation of a new batch or the sending of a finished batch when the the
     * maximum size limit is reached.
     * 
     * @param element the element to add
     */
    void enqueue(T element);

}

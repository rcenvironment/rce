/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import de.rcenvironment.core.monitoring.system.api.model.AverageOfDoubles;

/**
 * Implements a ring buffer of native double elements. No object wrapping is performed around the elements (as opposed to Commons
 * Collections CircularFifoBuffer, for example). This makes it CPU cache friendly, which keeps it suitable for constant background logging
 * and fairly high element counts.
 *
 * @author Robert Mischke
 */
public final class RingBufferOfDouble {

    private final double[] elements;

    private final int capacity; // redundant to elements.length, but better for clarity

    private int nextInsertPos;

    private int currentElementCount;

    public RingBufferOfDouble(int capacity) {
        this.capacity = capacity;
        this.elements = new double[capacity];
    }

    /**
     * Inserts the next element, evicting the oldest element if the buffer is already at capacity.
     * 
     * @param element the new element
     */
    public void add(double element) {
        elements[nextInsertPos] = element;
        nextInsertPos = (nextInsertPos + 1) % capacity;
        currentElementCount = Math.min(currentElementCount + 1, capacity);
    }

    /**
     * Returns the average value of a certain number of the most recent elements, together with information about how many elements were
     * actually included in the average. If that count is zero (because no elements have been recorded yet), the average should be
     * considered undefined; as a convention, it is set to 0.0.
     * 
     * @param maxElements the maximum number of elements to include
     * @return a holder of the average and the number of elements
     */
    public AverageOfDoubles getAverageOfLatest(final int maxElements) {
        final int numElementsToInclude = Math.min(maxElements, currentElementCount);
        if (numElementsToInclude == 0) {
            return new AverageOfDoubles(); // placeholder constructor
        }
        double sum = 0.0;
        for (int i = 0, pos = (nextInsertPos - numElementsToInclude + capacity) % capacity; i < numElementsToInclude; i++, pos =
            (pos + 1) % capacity) {
            sum += elements[pos];
        }
        return new AverageOfDoubles(numElementsToInclude, sum / numElementsToInclude);
    }

}

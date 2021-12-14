/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implements a synchronized general ring buffer. No object wrapping is performed around the elements (as opposed to Commons Collections
 * CircularFifoBuffer, for example). This makes it CPU cache friendly, which keeps it suitable for constant background logging and fairly
 * high element counts. Adapted from {@link RingBufferOfDouble} by Robert Mischke.
 *
 * @param <T> element type for the ring buffer.
 * @author Dominik Schneider
 */
//TODO validate performance (multi threading)
public class RingBuffer<T> {

    private final T[] elements;

    private final int capacity; // redundant to elements.length, but better for clarity

    private int nextInsertPos;

    private int currentElementCount;

    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.elements = (T[]) new Object[this.capacity];

    }

    /**
     * Inserts the next element, evicting the oldest element if the buffer is already at capacity.
     * 
     * @param element the new element
     */
    public synchronized void add(final T element) {
        elements[nextInsertPos] = element;
        nextInsertPos = (nextInsertPos + 1) % capacity;
        currentElementCount = Math.min(currentElementCount + 1, capacity);
    }

    /**
     * Get the latest added entry.
     * 
     * @return the latest added entry
     */
    public synchronized Optional<T> getLatestEntry() {
        if (this.currentElementCount < 1) {
            return Optional.empty();
        } else {
            return Optional.of(elements[getPreviousIndex(1)]);
        }
    }

    /**
     * Returns the last elements defined by the input. The elements are ordered by insert order (oldest element at [0]).
     * 
     * @param countOfElements the number of the last elements
     * @return an umodifiable list of the last specified elements
     */
    public synchronized List<T> getLastItems(int countOfElements) {
        if (this.currentElementCount > 0) {
            if (countOfElements > this.currentElementCount) {
                countOfElements = this.getCurrentElementCount();
            }
            @SuppressWarnings("unchecked") T[] copyOfElements = (T[]) new Object[countOfElements];
            int startIndex = getPreviousIndex(countOfElements); // prevents negative start indices
            int i = startIndex;
            for (int j = 0; j < countOfElements; j++) {
                copyOfElements[j] = elements[i];
                i = ++i % capacity;
            }
            return Collections.unmodifiableList(Arrays.asList(copyOfElements));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the number of elements in the ring buffer.
     * 
     * @return number of elements in the ring buffer
     */
    public synchronized int getCurrentElementCount() {
        return this.currentElementCount;
    }

    private synchronized int getPreviousIndex(final int stepsBack) {
        return (this.capacity + (this.nextInsertPos - stepsBack)) % capacity;
    }

}

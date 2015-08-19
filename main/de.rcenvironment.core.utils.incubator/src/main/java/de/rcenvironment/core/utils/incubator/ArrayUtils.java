/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.incubator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for Arrays.
 *
 * @author Arne Bachmann
 */
public final class ArrayUtils {
    
    /**
     * Hiding constructor.
     */
    private ArrayUtils() { }


    /**
     * Converts an array to a {@link Set}.
     * 
     * @param <T> the type
     * @param array the array to convert
     * @return the {@link Set}
     */
    public static <T> Set<T> toSet(T[] array) {
        final Set<T> result = new HashSet<T>();
        for (final T element : array) {
            result.add(element);
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Converts an array to a {@link List}.
     * 
     * @param <T> the type
     * @param array the array to convert
     * @return the {@link List}
     */
    public static <T> List<T> toList(T[] array) {
        final List<T> result = new ArrayList<T>(array.length);
        for (final T element : array) {
            result.add(element);
        }
        return result;
    }

}

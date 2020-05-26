/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;

/**
 * Defines the conversion between {@link TypedDatum} instances and a format suitable for persistence
 * and/or transmission.
 * 
 * @author Robert Mischke
 */
public interface TypedDatumSerializer {

    /**
     * Restores a {@link TypedDatum} from its serialized form.
     * 
     * @param input the serialized data
     * @return the restored {@link TypedDatum}
     * @throws IOException
     */
    TypedDatum deserialize(String input);

    /**
     * Converts a {@link TypedDatum} to its serialized form.
     * 
     * @param input the {@link TypedDatum}
     * @return the serialized data
     */
    String serialize(TypedDatum input);
}

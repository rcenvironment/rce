/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;

/**
 * Message-related utilities like serialization/deserialization.
 * 
 * @author Robert Mischke
 */
public final class MessageUtils {

    private static final int INITIAL_SERIALIZATION_BUFFER_SIZE = 512;

    private MessageUtils() {}

    /**
     * Serializes an object for sending it as a byte array.
     * 
     * @param object the object to serialize
     * @return the byte array form of the object
     * @throws SerializationException on serialization failure
     */
    public static byte[] serializeObject(Serializable object) throws SerializationException {
        return serialize(object);
    }

    /**
     * Serializes an object for sending it as a byte array, but unlike
     * {@link #serializeObject(Serializable)}, this method converts any
     * {@link SerializationException} into a {@link RuntimeException}. This is intended for objects
     * that were not received over the network, but generated locally instead. If such an object
     * fails to serialize, it is the result of a local programming error, where throwing a
     * {@link RuntimeException} is appropriate.
     * 
     * @param object the object to serialize
     * @return the byte array form of the object
     */
    public static byte[] serializeSafeObject(Serializable object) {
        try {
            return serialize(object);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes an object from a byte array and returns it as a {@link Serializable}. This
     * method should always be used when the serialized object may be of a non-primitive Java type
     * to ensure proper OSGi classloader access.
     * 
     * @param data the byte array form of the serialized object
     * 
     * @return the reconstructed object
     * @throws SerializationException on deserialization failure
     */
    public static Serializable deserializeObject(byte[] data) throws SerializationException {
        return deserializeObject(data, Serializable.class);
    }

    /**
     * Deserializes an object from a byte array, with an explicit return type parameter. This method
     * should always be used when the serialized object may be of a non-primitive Java type to
     * ensure proper OSGi classloader access.
     * 
     * @param data the byte array form of the serialized object
     * @param <T> the type of the object to restore
     * @param clazz the class of the object to restore
     * @return the reconstructed object
     * @throws SerializationException on deserialization failure
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeObject(byte[] data, Class<T> clazz) throws SerializationException {
        return (T) deserialize(data);
    }

    private static byte[] serialize(Serializable object) throws SerializationException {
        StatsCounter.countClass("MessageUtils.serialize()", object);
        
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream(INITIAL_SERIALIZATION_BUFFER_SIZE);
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
        } catch (IOException ex) {
            throw new SerializationException(ex);
        } finally {
            IOUtils.closeQuietly(oos);
        }
        return baos.toByteArray();
    }

    private static Object deserialize(byte[] data) throws SerializationException {
        if (data == null) {
            throw new SerializationException(new NullPointerException());
        }
        if (data.length == 0) {
            throw new SerializationException("Empty array passed for deserialization");
        }
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object object = ois.readObject();
            StatsCounter.countClass("MessageUtils.deserialize()", object);
            return object;
        } catch (ClassNotFoundException ex) {
            throw new SerializationException(ex);
        } catch (IOException ex) {
            throw new SerializationException(ex);
        } finally {
            IOUtils.closeQuietly(ois);
        }
    }

}

/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A typed map, disallowing wrong types after initial put operations.
 * Null is not allowed, neither as key nor as value.
 *
 * @param <KeyType> The key of the data structure
 * @version $LastChangedRevision: 0$
 * @author Arne Bachmann
 */
public class TypedProperties<KeyType extends Serializable> {

    /**
     * Contains all values.
     */
    private Map<KeyType, Serializable> values = new LinkedHashMap<KeyType, Serializable>();

    /**
     *  Contains the type constraints of all values.
     */
    private Map<KeyType, Class<? extends Serializable>> types = new LinkedHashMap<KeyType, Class<? extends Serializable>>();

    /**
     * Number of elements in the properties map.
     * 
     * @return The number of elements
     */
    public int size() {
        return values.size();
    }

    /**
     * Put a typed value into the map.
     * 
     * @param propertyName The name (key) of the property
     * @param value The value to put
     * @exception IllegalStateException If the provided type differs from an already contained
     *            value with the same key
     */
    public void put(final KeyType propertyName, final Serializable value) throws IllegalStateException {
        put(propertyName, value, /* raise exception if type differs */ true);
    }

    /**
     * Put a typed value into the map.
     * 
     * @param propertyName The name (key) of the property
     * @param value The value to put
     * @param throwException If existing type differs from provided one, true means throwing an
     *        exception, false overwrites
     * @exception IllegalStateException If the provided type differs from an already contained
     *            value with the same key
     */
    protected void put(final KeyType propertyName, final Serializable value, final boolean throwException) throws IllegalStateException {
        assert propertyName != null;
        assert value != null;
        final Class<? extends Serializable> newType = value.getClass();
        if (throwException) {
            final Class<? extends Serializable> type = types.get(propertyName);
            if (type != null) {
                if ((type != newType) && (!(type.isAssignableFrom(newType)))) { // incompatible type
                    throw new IllegalStateException("The provided type differs from the type already contained or not a subclass");
                }
            } else {
                types.put(propertyName, newType);
            }
        } else {
            types.put(propertyName, newType);
        }
        values.put(propertyName, value);
    }
    
    /**
     * If you want to ensure the correct type for a key, but no value is available yet, use this method.
     * The value will be null, but the type will be enforced with every set
     * 
     * @param propertyName The key
     * @param type The desired type
     */
    public void setType(final KeyType propertyName, final Class<? extends Serializable> type) {
        assert propertyName != null;
        assert type != null;
        values.put(propertyName, null);
        types.put(propertyName, type);
    }

    /**
     * Get the stored value of the expected type. If the stored type differs, we throw an exception
     * 
     * @param propertyName The name of the property to get
     * @param clazz The expected type
     * @return The value or null if not found
     * @exception IllegalStateException If the stored type differs from the requested one
     * @param <U> type to which the property is restricted
     */
    @SuppressWarnings("unchecked")
    public <U extends Serializable> U get(final KeyType propertyName, final Class<U> clazz) throws IllegalStateException  {
        assert propertyName != null;
        assert clazz != null;
        final Class<? extends Serializable> type = types.get(propertyName);
        if (type == null) {
            return null;
        }
        if ((clazz != type) && !clazz.isAssignableFrom(type)) {
            throw new IllegalStateException("The contained type differs from the type requested");
        }
        return (U) values.get(propertyName); // this cast is necessary, but safe
    }

    /**
     * Check property existence.
     * 
     * @param propertyName The name to check
     * @return True if contained
     */
    public boolean containsKey(final KeyType propertyName) {
        return values.containsKey(propertyName);
    }
    
    /**
     * Remove the entry.
     * 
     * @param propertyName The entry name to remove
     * @return True if the entry name was found
     */
    public boolean remove(final KeyType propertyName) {
        final boolean found = (values.remove(propertyName) != null) || (propertyName == null);
        types.remove(propertyName);
        return found;
    }
    
    /**
     * Remove the entry, if it has the right type, otherwise return false.
     * 
     * @param propertyName The entry name to remove
     * @param clazz The desired type to remove
     * @return True if removed, false if not
     */
    public boolean remove(final KeyType propertyName, final Class<? extends Serializable> clazz) {
        if (!containsKey(propertyName) || !hasType(propertyName, clazz)) {
            return false;
        }
        return remove(propertyName);
    }

    /**
     * Check if the contained value is of the right type.
     * 
     * @param propertyName The property to check
     * @param clazz The assumed class type
     * @return True if the contained class type is the same as the provided one
     */
    public boolean hasType(final KeyType propertyName, final Class<? extends Serializable> clazz) {
        final Class<? extends Serializable> type = types.get(propertyName);
        if (type == null) {
            throw new IllegalStateException("Property not contained");
        }
        return clazz.isAssignableFrom(type);
    }

    /**
     * Get the contained type of the property.
     * 
     * @param propertyName Name of the property
     * @return The type of the property
     */
    public Class<? extends Serializable> getType(final KeyType propertyName) {
        return types.get(propertyName);
    }

    /**
     * Return the keys of all properties in an ordered set.
     * 
     * @return The set containing all keys, usable in foreach loops
     */
    public Set<KeyType> keySet() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /**
     * Return the set containing all values.
     * 
     * @return The values set
     */
    public Set<Entry<KeyType, Serializable>> valuesEntrySet() {
        return Collections.unmodifiableSet(values.entrySet());
    }

    /**
     * Return the set containing all types.
     * 
     * @return The types set
     */
    public Set<Entry<KeyType, Class<? extends Serializable>>> typesEntrySet() {
        return Collections.unmodifiableSet(types.entrySet());
    }

    /**
     * Empty the properties.
     */
    public void clear() {
        values.clear();
        types.clear();
    }

    /**
     * Add all entries in the map to the properties.
     * 
     * @param map The entries to add
     * @param disallowWrongTypes
     * @exception IllegalStateException If disalloWrongTypes is true and if any entries had the
     *            wrong type (after adding all).
     */
    protected void addAll(final Map<KeyType, Serializable> map, final boolean disallowWrongTypes) {
        IllegalStateException exception = null;
        for (final Entry<KeyType, Serializable> entry : map.entrySet()) {
            try {
                put(entry.getKey(), entry.getValue(), /* throw exception? */disallowWrongTypes);
            } catch (final IllegalStateException e) {
                exception = e;
            }
        }
        if (exception != null) {
            throw new IllegalStateException("At least one entry in the provided map had the wrong type. First occurrence: "
                + exception.getMessage());
        }
    }

    /**
     * Add all entries in the map to the properties, throwing an exception if some provided types
     * had the wrong type (not overwriting existing values).
     * 
     * @param map The entries to add
     * @exception IllegalStateException If any entries had the wrong type (after adding all).
     */
    public void addAll(final Map<KeyType, Serializable> map) throws IllegalStateException {
        addAll(map, /* throw exception */true);
    }

    /**
     * Add all entries in the map to the properties, overwriting existing ones.
     * 
     * @param map The entries to add
     */
    public void addAllOverwriting(final Map<KeyType, Serializable> map) {
        addAll(map, /* throw exception */false);
    }

    /**
     * Add all entries in the map to the properties.
     * 
     * @param properties The entries to add
     * @param disallowWrongTypes
     * @exception IllegalStateException If disalloWrongTypes is true and if any entries had the
     *            wrong type (after adding all).
     */
    protected void addAll(final TypedProperties<KeyType> properties, final boolean disallowWrongTypes) throws IllegalStateException {
        IllegalStateException exception = null;
        for (final KeyType propertyName: properties.keySet()) {
            final Class<? extends Serializable> type = properties.getType(propertyName);
            final Serializable value = (Serializable) properties.get(propertyName, type);
            try {
                get(propertyName, type);
            } catch (final IllegalStateException e) {
                if (disallowWrongTypes) {
                    exception = e;
                    continue;
                }
            }
            put(propertyName, value, false); // no need to check here, we did it already by get
        }
        if (exception != null) {
            throw new IllegalStateException("At least one entry in the provided properties had the wrong type. First occurrence: "
                + exception.getMessage());
        }
    }

    /**
     * Add all entries in the map to the properties, throwing an exception if some provided types
     * had the wrong type (not overwriting existing values).
     * 
     * @param properties The entries to add
     * @exception IllegalStateException If any entries had the wrong type (after adding all).
     */
    public void addAll(final TypedProperties<KeyType> properties) throws IllegalStateException {
        addAll(properties, /* throw exception */true);
    }

    /**
     * Add all entries in the map to the properties, overwriting existing ones.
     * 
     * @param properties The entries to add
     */
    public void addAllOverwriting(final TypedProperties<KeyType> properties) {
        addAll(properties, /* throw exception */false);
    }

    /**
     * Use this method to check if all properties necessary for e.g. an execution are provided and
     * have the right types.
     * 
     * @param typesToCheck The property name -> property type to check for
     * @exception IllegalStateException If any entries has the wrong type
     */
    public void hasValidTypes(final Map<KeyType, Class<? extends Serializable>> typesToCheck) throws IllegalStateException {
        for (final Entry<KeyType, Class<? extends Serializable>> type: typesToCheck.entrySet()) {
            if (!typesToCheck.containsKey(type.getKey())) {
                throw new IllegalStateException("Missing property " + type.getKey() + " in execution context");
            } else if (typesToCheck.get(type.getKey()) != type.getValue()) {
                throw new IllegalStateException("Property " + type.getKey() + " has wrong type "
                    + type.getClass().getTypeParameters().toString());
            }
        }
    }

}

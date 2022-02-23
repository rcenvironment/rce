/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A part (or all) of the managed hierarchical configuration data.
 * 
 * @author Robert Mischke
 */
public interface ConfigurationSegment {

    /**
     * Fetches a part of the current configuration segment, as defined by the relative path argument. For example, if the current segment is
     * "network", calling this method with "ipFilter" will result the segment for the "network/ipFilter" path. Multiple path levels can be
     * concatenated with slashes.
     * <p>
     * If no entry/segment exists at the given path, this method will still return a {@link ConfigurationSegment}, but that instance will
     * return <code>false</code> on calls to {@link #isPresentInCurrentConfiguration()}.
     * 
     * @param relativePath the path relative to the current one to fetch
     * @return the requested {@link ConfigurationSegment}
     */
    ConfigurationSegment getSubSegment(String relativePath);

    /**
     * @param relativePath the path relative to the current one to fetch
     * @return the value of the given relativ path, or null if no such value exists
     * @throws ConfigurationException on failure.
     */
    List<String> getStringArray(String relativePath) throws ConfigurationException;

    /**
     * Fetches a part of the current configuration segment, as defined by the relative path argument. For example, if the current segment is
     * "network", calling this method with "ipFilter" will result the segment for the "network/ipFilter" path. Multiple path levels can be
     * concatenated with slashes.
     * <p>
     * If no entry/segment exists at the given path, this method will create the missing elements as needed. The returned
     * {@link WritableConfigurationSegment} can then be used to edit the properties or elements of that segment. Note that the current
     * segment must already exist; otherwise, an {@link IOException} is thrown.
     * 
     * @param relativePath the path relative to the current one to fetch
     * @return the requested {@link ConfigurationSegment}
     * @throws ConfigurationException if the current segment does not actually exist, or if an error occurs while adding the elements
     */
    WritableConfigurationSegment getOrCreateWritableSubSegment(String relativePath) throws ConfigurationException;

    /**
     * @param relativePath the value's relative path to the current segment
     * @return the value at the given relative path, or null if no such value exists
     */
    String getString(String relativePath);

    /**
     * @param relativePath the value's relative path to the current segment
     * @param defaultValue the default value to return
     * @return the value at the given relative path, or the given default if no such value exists
     */
    String getString(String relativePath, String defaultValue);

    /**
     * @param relativePath the value's relative path to the current segment
     * @return the value at the given relative path, or null if no such value exists or if it cannot be parsed
     */
    Long getLong(String relativePath);

    /**
     * @param relativePath the value's relative path to the current segment
     * @param defaultValue the default value to return
     * @return the value at the given relative path, or the given default if no such value exists or if it cannot be parsed
     */
    Long getLong(String relativePath, Long defaultValue);

    /**
     * @param relativePath the value's relative path to the current segment
     * @return the value at the given relative path, or null if no such value exists
     */
    Integer getInteger(String relativePath);

    /**
     * @param relativePath the value's relative path to the current segment
     * @param defaultValue the default value to return
     * @return the value at the given relative path, or the given default if no such value exists
     */
    Integer getInteger(String relativePath, Integer defaultValue);

    /**
     * @param relativePath the value's relative path to the current segment
     * @return the value at the given relative path, or null if no such value exists
     */
    Double getDouble(String relativePath);

    /**
     * @param relativePath the value's relative path to the current segment
     * @param defaultValue the default value to return
     * @return the value at the given relative path, or the given default if no such value exists
     */
    Double getDouble(String relativePath, Double defaultValue);

    /**
     * @param relativePath the value's relative path to the current segment
     * @return the value at the given relative path, or null if no such value exists
     */
    Boolean getBoolean(String relativePath);

    /**
     * @param relativePath the value's relative path to the current segment
     * @param defaultValue the default value to return
     * @return the value at the given relative path, or the given default if no such value exists
     */
    Boolean getBoolean(String relativePath, Boolean defaultValue);

    /**
     * Returns true if the current segment exists in the configuration it was created from. THe root segment will return true as long as the
     * underlying storage (typically a file) exists. Sub-segments will return true if the path used to create them from actually points to
     * an entry/segment in the configuration hierarchy.
     * 
     * @return true if the current segment exists in the configuration it was created from
     */
    boolean isPresentInCurrentConfiguration();

    // List<String> getStringArray(String relativePath);

    // ConfigurationSegment getElement(String relativePath, String id);

    /**
     * Lists all JSON field-value pairs at the given location, with each value part mapped as a {@link ConfigurationSegment}. For
     * convenience in typical configuration scenarios, using this method on a non-existing path returns an empty list, just as if the path
     * existed with no entries.
     * 
     * @param relativePath the relative path to the current segment to read
     * @return the map of field names to configuration sub-segments
     */
    Map<String, ConfigurationSegment> listElements(String relativePath);

    /**
     * Maps the given segment to an instance of the given bean class. See {@link ObjectMapper} for mapping details.
     * 
     * @param <T> the bean class to map to
     * @param clazz the bean class to map to
     * @return the mapped object
     * @throws IOException on parsing or mapping errors
     */
    <T> T mapToObject(Class<T> clazz) throws IOException;

}

/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.eventlog.api.EventLogEntry;
import de.rcenvironment.core.eventlog.api.EventType;

/**
 * Convenience and type safety wrapper for constructing new log entries.
 *
 * @author Robert Mischke
 */
public final class EventLogEntryImpl implements EventLogEntry {

    /**
     * The expected length of {@link #generateHumanReadableLocalTimestamp(LocalDateTime)} return values.
     */
    public static final int EXPECTED_HUMAN_READABLE_TIMESTAMP_LENGTH = 23;

    // "immutable and thread-safe", according to JavaDoc
    private static final DateTimeFormatter sharedHumanReadableTimestampFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");

    private static final ObjectMapper sharedJsonMapper = new ObjectMapper();

    private final EventType eventType;

    private final Instant timestamp;

    private final Map<String, String> attributeData;

    public EventLogEntryImpl(EventType eventType) {
        this.eventType = Objects.requireNonNull(eventType);
        this.timestamp = Instant.now();
        this.attributeData = new HashMap<>();
    }

    public EventLogEntryImpl(Instant timestamp, EventType eventType, Map<String, String> attributeData) {
        this.eventType = Objects.requireNonNull(eventType);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.attributeData = Objects.requireNonNull(attributeData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventLogEntry set(String key, String value) {
        attributeData.put(key, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventLogEntry set(String key, long value) {
        set(key, Long.toString(value));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventLogEntry set(String key, int value) {
        set(key, Integer.toString(value));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventLogEntry setAll(Map<String, String> dataMap) {
        attributeData.putAll(dataMap);
        return this;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public String getEventTypeId() {
        return eventType.getId();
    }

    @Override
    public String getEventTypeTitle() {
        return eventType.getTitle();
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, String> getAttributeData() {
        return attributeData;
    }

    public String getHumanReadableLocalTimestamp() {
        return generateHumanReadableLocalTimestamp(timestamp);
    }

    public String getAttributeDataAsConpactJsonString() {
        return generateCompactJsonString(attributeData);
    }

    /**
     * Convenience/standardization function to render key-value data to a compact JSON string.
     * 
     * @param data the key/value map to render
     * @return the JSON string representation
     */
    public static String generateCompactJsonString(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        } else {
            try {
                return sharedJsonMapper.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                // fallback in unlikely case of JSON encoding error
                throw new RuntimeException("Unexpected failure: JSON encoding of event data failed", e);
            }
        }
    }

    /**
     * Convenience/standardization function to render a {@link LocalDateTime} to a human-readable timestamp of a common, default format.
     * This format is based on ISO 8601, with millisecond accuracy and "unqualified local time", i.e., no timezone information.
     * 
     * @param instant the UTC-like timestamp as a Java {@link Instant}
     * @return the rendered timestamp string
     */
    public static String generateHumanReadableLocalTimestamp(Instant instant) {

        // convert to localized timestamp
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

        // format
        String formatted = localDateTime.format(sharedHumanReadableTimestampFormatter);

        int len = formatted.length();
        if (len == EXPECTED_HUMAN_READABLE_TIMESTAMP_LENGTH) {
            return formatted;
        } else {
            throw new IllegalStateException(Integer.toString(len)); // sanity check failed
        }
    }

}

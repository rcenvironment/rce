/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog;

/**
 * Interface of common RCE event log operations. Subinterfaces may extend these with
 * context-specific operations.
 * 
 * @author Robert Mischke
 * 
 */
public interface EventLogger {

    /**
     * Logs an error event, similar to {@link Log#error(Object)}. In contrast to warnings, errors
     * should always indicate an operation that failed unexpectedly.
     * 
     * @param localized true if the given message should be treated as a message id; false, if it is
     *        a non-localized message
     * @param message the message or message id to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void error(boolean localized, String message, Object... parameters);

    /**
     * Logs an error event with detail information, similar to {@link Log#error(Object, Throwable)}.
     * In contrast to warnings, errors should always indicate an operation that failed unexpectedly.
     * 
     * @param detailInformation the detailed technical reason for the error, ie the underlying
     *        {@link Throwable}
     * @param localized true if the given message should be treated as a message id; false, if it is
     *        a non-localized message
     * @param message the message or message id to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void error(Throwable detailInformation, boolean localized, String message, Object... parameters);

    /**
     * Logs a warning, similar to {@link Log#warn(Object)}. In contrast to errors, warnings should
     * indicate an abnormal situation that did not result in an unrecoverable error.
     * 
     * @param localized true if the given message should be treated as a message id; false, if it is
     *        a non-localized message
     * @param message the message or message id to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void warn(boolean localized, String message, Object... parameters);

    /**
     * Logs a warning with detail information, similar to {@link Log#warn(Object, Throwable)}. In
     * contrast to errors, warnings should indicate an abnormal situation that did not result in an
     * unrecoverable error.
     * 
     * @param detailInformation the detailed technical reason for the error, ie the underlying
     *        {@link Throwable}
     * @param localized true if the given message should be treated as a message id; false, if it is
     *        a non-localized message
     * @param message the message or message id to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void warn(Throwable detailInformation, boolean localized, String message, Object... parameters);

    /**
     * Logs an informational message, similar to {@link Log#info(Object)}. As "info" messages should
     * never reflect errors, there is no equivalent method with an additional {@link Throwable}
     * parameter.
     * 
     * @param localized true if the given message should be treated as a message id; false, if it is
     *        a non-localized message
     * @param message the message or message id to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void info(boolean localized, String message, Object... parameters);

    /**
     * Logs an internal debug message. As debug messages are aimed at developers instead of end
     * users, they are not localized.
     * 
     * @param message the message to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void debug(String message, Object... parameters);

    /**
     * Logs an internal debug message with detail information. As debug messages are aimed at
     * developers instead of end users, they are not localized.
     * 
     * @param detailInformation the detailed technical reason for the error, ie the underlying
     *        {@link Throwable}
     * @param message the message to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void debug(Throwable detailInformation, String message, Object... parameters);

    /**
     * Logs a fine-grained internal debug message, similar to the "trace" level of some logging
     * frameworks. Unlike "debug" messages, which are always stored, these "verbose debug" messages
     * may be discarded, depending on configuration values.
     * 
     * Note: For performance reasons, callers should check {@link #isDebugVerboseEnabled()} first,
     * and only make the actual verbose log calls if it returned "true".
     * 
     * @param message the message to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void debugVerbose(String message, Object... parameters);

    /**
     * Logs a fine-grained internal debug message with detail information, similar to the "trace"
     * level of some logging frameworks. Unlike "debug" messages, which are always stored, these
     * "verbose debug" messages may be discarded, depending on configuration values.
     * 
     * Note: For performance reasons, callers should check {@link #isDebugVerboseEnabled()} first,
     * and only make the actual verbose log calls if it returned "true".
     * 
     * @param detailInformation the detailed technical reason for the error, ie the underlying
     *        {@link Throwable}
     * @param message the message to display; messages can contain
     *        {@link String#format(String, Object...)} placeholders for the given parameters
     * @param parameters the parameters to insert into the given message, or the message resolved
     *        from the given message id if localization is enabled
     */
    void debugVerbose(Throwable detailInformation, String message, Object... parameters);

    /**
     * Returns whether verbose debug logging is enabled or not. Allows clients to skip verbose
     * logging operations to improve performance.
     * 
     * @return true if verbose logging is enabled
     */
    boolean isDebugVerboseEnabled();
}

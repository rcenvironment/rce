/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

/**
 * Exception class representing monitoring failures.
 * 
 * @author David Scholz
 * @author Robert Mischke (adapted for remote usage/reconstruction)
 */
public class OperatingSystemException extends Exception {

    private static final long serialVersionUID = -3136170788474556276L;

    private final ErrorType errorType;

    // for client-side reconstruction after remote exception throw
    public OperatingSystemException(String message) {
        super(message);
        this.errorType = null; // not available in remote case
    }

    public OperatingSystemException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public OperatingSystemException(ErrorType errorType, String message) {
        super(errorType.getMessage() + ": " + message);
        this.errorType = errorType;
    }

    /**
     * Factory method for process-related ACCESS_DENIED exceptions.
     * 
     * @param pid the PID of the inaccessible process
     * @return the generated exception
     */
    public static OperatingSystemException createAccessDeniedException(long pid) {
        return new OperatingSystemException(OperatingSystemException.ErrorType.ACCESS_DENIED, " to process with pid: " + pid
            + ". You may not have the appropriate permissions.");
    }

    /**
     * Factory method for NO_SUCH_PROCESS exceptions.
     * 
     * @param pid the PID of the missing process
     * @return the generated exception
     */
    public static OperatingSystemException createNoSuchProcessException(long pid) {
        return new OperatingSystemException(OperatingSystemException.ErrorType.NO_SUCH_PROCESS, ". The process with pid: " + pid
            + " may not exist.");
    }

    /**
     * @return the {@link ErrorType} passed to the constructor; note that this method MUST NOT be used if the exception was received from a
     *         remote instance - if it is, an {@link IllegalStateException} is thrown
     */
    public ErrorType getErrorType() {
        if (errorType == null) {
            throw new IllegalStateException("Error type requested, but none is available; "
                + "was this method called on a different instance than the one throwing the exception?");
        }
        return errorType;
    }

    /**
     * 
     * Error codes.
     * 
     * @author David Scholz
     */
    public enum ErrorType {
        /**
         * No such process error.
         */
        NO_SUCH_PROCESS("No such process"),

        /**
         * Error if getting cpu usage of a specific process fails.
         */
        FAILED_TO_GATHER_CPU_PROCESS_USAGE("Failed to get CPU usage"),

        /**
         * Error if getting idle fails.
         */
        FAILED_TO_GATHER_IDLE("Failed to get idle"),

        /**
         * Error if getting total cpu usage fails.
         */
        FAILED_TO_GATHER_TOTAL_CPU_USAGE("Failed to get total CPU usage"),

        /**
         * Error if getting ram usage of a specific process fails.
         */
        FAILED_TO_GATHER_RAM_PROCESS_USAGE("Failed to get RAM usage"),

        /**
         * Error if getting total ram usage fails.
         */
        FAILED_TO_GATHER_TOTAL_RAM_USAGE("Failed to get total RAM usage"),

        /**
         * Error if getting total system ram fails.
         */
        FAILED_TO_GATHER_TOTAL_RAM("Failed to get total RAM"),

        /**
         * Error if getting total ram percentage fails.
         */
        FAILED_TO_GATHER_TOTAL_RAM_PERCENTAGE("Failed to get total RAM percentage"),

        /**
         * Error if getting ram percentage of specific process fails.
         */
        FAILED_TO_GATHER_RAM_PROCESS_PERCENTAGE("Failed to get RAM percentage"),

        /**
         * Error if user does not have the proper rights to get access to the process.
         */
        ACCESS_DENIED("Access is denied"),

        /**
         * Error if getting process list fails.
         */
        FAILED_TO_GET_PROCESS_LIST("Failed to get process list"),

        /**
         * Error if getting cpu list fails.
         */
        FAILED_TO_GET_CPU_LIST("Failed to get cpu core count"),

        /**
         * Error if getting process state fails.
         */
        FAILED_TO_GET_PROCESS_STATE("Failed to get process state"),

        /**
         * Error if getting children fails.
         */
        FAILED_TO_GET_CHILD_PROCESS_LIST("Failed to get child process list"),

        /**
         * Error if killing process fails.
         */
        FAILED_TO_KILL_PROCESS("Failed to kill process"),

        /**
         * Error if getting file system information fails.
         */
        FAILED_TO_GET_FILE_SYSTEM("Failed to get file system information"),

        /**
         * Error if getting file system usage fails.
         */
        FAILED_TO_GET_FILE_SYSTEM_USAGE("Failed to get file system usage");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

}

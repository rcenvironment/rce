/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.extras.testscriptrunner.definitions.helper;

import java.util.regex.Pattern;

/**
 * @author Marlon Schroeter
 * @author Robert Mischke (minor changes)
 */
public final class StepDefinitionConstants {
    
    /*
     * -------------------------------------------------------------------------------------------------
     * THE CONSTANTS BELOW NEED TO BE UPDATED AFTER EACH MAJOR RELEASE TO CONTAIN THE LATEST INFORMATION
     * 
     * When moving them elsewhere, update the location of these constants in the internal Wiki
     * -------------------------------------------------------------------------------------------------
     */
    
    /**
     * remote file URL of previous major version. Asterisk is replaced by OS depending term automatically.
     * 
     * TODO hardcoding the build identifier should not be necessary; this should be investigated and changed - misc_ro
     */
    public static final String LEGACY_URL =
        "https://software.dlr.de/updates/rce/9.x/products/standard/releases/latest/zip/"
        + "rce-9.1.1.201907260521-standard-*.x86_64.zip";

    /**
     * legacy version used in tests between major versions.
     */
    public static final int LEGACY_VERSION = 9;

    /**
     * remote file url of earliest minor version. Asterisk is replaced by OS depending term automatically.
     * 
     * TODO hardcoding the build identifier should not be necessary; this should be investigated and changed - misc_ro
     */
    public static final String BASE_URL =
        "https://software.dlr.de/updates/rce/10.x/products/standard/releases/10.0.0/zip/"
        + "rce-10.0.0.201911221509-standard-*.x86_64.zip";

    /**
     * base version used in tests between minor versions.
     */
    public static final int BASE_VERSION = 10;

    /*
     * -------------------------------------------------------------------------------------------------
     * IMMUTABLE CONSTANTS
     * -------------------------------------------------------------------------------------------------
     */
    
    /**
     * string representing abstract component.
     */
    public static final String ABSENT_COMPONENT_STRING = "(absent)";

    /**
     * regex matching any string containing at least one character.
     */
    public static final String ANY_STRING = ".+";

    /**
     * regex matching any package.
     */
    public static final String ANY_PACKAGE = "[\\w\\.]+";
    
    /**
     * format for concatenating serverInstanceId and UserName to create unique connection identifier.
     */
    public static final String CONNECTION_ID_FORMAT = "%s_%s";

    /**
     * constant for the abbreviation for a regular connection.
     */
    public static final String CONNECTION_TYPE_REGULAR = "reg";

    /**
     * constant for the abbreviation for a shh connection.
     */
    public static final String CONNECTION_TYPE_SSH = "ssh";

    /**
     * constant for the abbreviation for a uplink connection.
     */
    public static final String CONNECTION_TYPE_UPLINK = "upl";

    /**
     * format for concatenating rootcommand and subcommand in command description.
     */
    public static final String COMMAND_DESCRIPTION_FORMAT = "%s %s";

    /**
     * name of debug file.
     */
    public static final String DEBUG_LOG_FILE_NAME = "debug.log";

    /**
     * escaped double quote.
     */
    public static final String ESCAPED_DOUBLE_QUOTE = "\"";

    /**
     * format for presenting an error message regarding unsupported connection types.
     */
    public static final String ERROR_MESSAGE_UNSUPPORTED_TYPE = "Type %s is not supported.";

    /**
     * format for presenting an error message regarding type difference between option and received parameter.
     */
    public static final String ERROR_MESSAGE_WRONG_TYPE = "Value %s is of a wrong type to be applied to option %s";

    /**
     * message content regarding correctly extracted log output directory.
     */
    public static final String FOUND_LOG_OUTPUT_DIRECTORY_LOCATION = "Found log output directory location '";
    
    /**
     * format for concatenating host and port.
     */
    public static final String HOST_PORT_FORMAT = "%s:%s";

    /**
     * subfolder under IM rootfolder in which legacy versions are to be installed.
     */
    public static final String INSTALLATION_SUBFOLDER_NAME_LEGACY = "_legacy";

    /**
     * subfolder under IM rootfolder in which base major versions are to be installed.
     */
    public static final String INSTALLATION_SUBFOLDER_NAME_BASE = "_base_major";
    
    /**
     * regex matching single linebreak.
     */
    public static final String LINEBREAK_REGEX = "(\r\n|\r|\n)";

    /**
     * regex format for matching a specific error. Origin of Error and Error Message necessary to match.
     */
    public static final String LOG_CONTAINS_ERROR_FORMAT = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} ERROR - %s - E#\\d+: %s";

    /**
     * regex format for matching a specific error. Origin of Error and Error Message necessary to match.
     */
    public static final String LOG_CONTAINS_WARNING_FORMAT = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} WARN  - %s - %s";

    /**
     * separates option key from option value.
     */
    public static final String OPTION_SEPARATOR = "_";

    /**
     * char to be replaced by os specific info.
     */
    public static final String REPLACEMENT_CHAR = "*";

    /**
     * string to replace asterix for linux.
     */
    public static final String REPLACEMENT_LINUX = "linux";
    
    /**
     * string to replace asterix for windows.
     */
    public static final String REPLACEMENT_WINDOWS = "win32";

    /**
     * slash char.
     */
    public static final String SLASH = "/";
    
    /**
     * success massage printed upon successful comparison of exported workflow runs, which are identical.
     */
    public static final String SUCCESS_MESSAGE_WORKFLOW_COMPARISON_IDENTICAL = "The compared workflow runs are identical.";
    
    /**
     * success massage printed upon successful comparison of exported workflow runs, which are different.
     */
    public static final String SUCCESS_MESSAGE_WORKFLOW_COMPARISON_DIFFERENT = "The compared workflow runs differ in the following way:";
    
    /**
     * succes message printed upon successful export of a workflow run.
     */
    public static final String SUCCESS_MESSAGE_WORKFLOW_EXPORT = "Successfully exported workflowrun.";

    /**
     * string used in step definition to check if substring shall be present or absent.
     */
    public static final String USE_NEGATION_MARKER = "not "; // could be any non-null string

    /**
     * string used in step definition to check if string shall be interpreted as such or regex.
     */
    public static final String USE_REGEXP_MARKER = "the pattern "; // could be any non-null string

    /**
     * name of file containing all warnings and errors.
     */
    public static final String WARNINGS_LOG_FILE_NAME = "warnings.log";

    /**
     * white space separator constant.
     */
    public static final String WHITESPACE_SEPARATOR = "\\s";

    /**
     * zip extension.
     */
    public static final String ZIP = ".zip";

    /**
     * expected length of workflow info array.
     */
    public static final int EXPECTED_WORKFLOW_INFO_LENGTH = 3;

    /**
     * default sleep/wait time in milliseconds.
     */
    public static final int SLEEP_DEFAULT_IN_MILLISECS = 500;

    /**
     * default timeout value for one bdd action in seconds.
     */
    public static final int IM_ACTION_TIMEOUT_IN_SECS = 60; // in seconds

    /**
     * pattern extracting id from string.
     */
    public static final Pattern INSTANCE_DEFINITION_ID_SUBPATTERN = Pattern.compile("Id=(\\w+)"); // length intentionally undefined

    /**
     * pattern for instance definition.
     */
    public static final Pattern INSTANCE_DEFINITION_PATTERN = Pattern.compile("(\\w+)( \\[.*\\])?");

    private StepDefinitionConstants() {
        // not used, since this class only provides constants
    }
}

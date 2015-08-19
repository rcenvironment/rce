/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

/**
 * Used to have a single point to add/ read constants.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public final class SshConstants {

    // CONSOLE STRINGS AND CONFIGURATION VALUES - START

    /**
     * Template for the interactive shell prompt.
     */
    public static final String CONSOLE_PROMPT_TEMPLATE = "\r\n%s@RCE: ";

    /**
     * Constant for exit command.
     */
    public static final String EXIT_COMMAND = "exit";

    /**
     * Constant for help command.
     */
    public static final String HELP_COMMAND = "help";

    /**
     * Constant for exit command.
     */
    public static final String DEFAULT_COMMANDS = "help|exit";

    /**
     * Constant for scp command.
     */
    public static final String SCP_COMMAND = "scp";

    /**
     * Constant for the name of the configuration file.
     */
    public static final String CONFIGURATION_NAME = "de.rcenvironment.core.embedded.ssh";

    /**
     * Constant for the name of the configuration file.
     */
    public static final String DEFAULT_ROLE_PRIVILEGES = ".*";

    /**
     * Lowest possible port. used to check if configured port is valid.
     */
    public static final int MIN_PORT_NUMBER = 0;

    /**
     * Highest possible port. used to check if configured port is valid.
     */
    public static final int MAX_PORT_NUMBER = 65535;

    /**
     * The shift for the write method in ConsoleAuthenticator.
     */
    public static final int AUTH_SHIFT = 24;

    /**
     * The int for the write method in ConsoleAuthenticator.
     */
    public static final int AUTH_INT = 0xFF;

    /**
     * The int for the write method in ConsoleAuthenticator.
     */
    public static final int COMMAND_HISTORY_SIZE = 20;

    // CONSOLE STRINGS AND CONFIGURATION VALUES - END
    // ASCII CODES - START

    /**
     * Code of the return key.
     */
    public static final int RETURN_KEY_CODE = 13;

    /**
     * Code of the del key.
     */
    public static final int DEL_KEY_CODE = 127;

    /**
     * Code of the tab key.
     */
    public static final int TAB_KEY_CODE = 9;

    /**
     * Lowest useful char for console.
     */
    public static final int LOWEST_USEFUL_KEY_CODE = 32;

    /**
     * Code for telling CommandBuilder to not add or printout the given sign.
     */
    public static final int NO_ADDING_INT_CODE = -1;

    /**
     * Code for the ESC key. Important because it is used as escape character for special keys
     */
    public static final int ESC_KEY_CODE = 27;

    // ASCII CODES - END
    // REGULAR EXPRESSIONS FOR SPECIAL KEY HANDLING - START

    /**
     * Regular expression to validate, if it could be a special key string or not.
     */
    public static final String SPECIAL_KEY_REGEX = "\\[([A-D]?|([1-6]([~0-9]~?)?)?)";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_LEFT = "\\[D";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_RIGHT = "\\[C";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_UP = "\\[A";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_DOWN = "\\[B";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_ENTF = "\\[3~";

    /**
     * Regular expression for F1, F2,..., F12, einfg, arrow down, arrow up, pos1, end, bup, bdown.
     */
    public static final String SPECIAL_KEYS_IGNORE_LIST = "\\[[12][0-9]~|\\[2~|\\[1~|\\[4~|\\[5~|\\[6~";

    // REGULAR EXPRESSIONS FOR SPECIAL KEY HANDLING - END

    /**
     * Length of the random parts of the username for temporary accounts.
     */
    public static final int TEMP_USER_NAME_RANDOM_LENGTH = 6;

    /**
     * Length of the random passwords for temporary accounts.
     */
    public static final int TEMP_USER_PASSWORD_RANDOM_LENGTH = 6;

    /**
     * Prefix for temporary user accounts.
     */
    public static final String TEMP_USER_PREFIX = "t_";

    private SshConstants() {}
}

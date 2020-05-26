/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import org.apache.commons.logging.LogFactory;

/**
 * Enumeration for operating system "families". Also provides a convenience method to determine the OS family the local instance is running
 * on.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public enum OSFamily {

    /**
     * All kinds of Microsoft Windows OSs.
     */
    Windows,

    /**
     * All kinds of unix, linux, apple, solaris and so on.
     */
    Linux,

    /**
     * Default when nothing was selected.
     */
    Unspecified;

    /**
     * Holder class that is initialized on first access; required because of enum initialization order rules.
     * 
     * @author Robert Mischke
     */
    private static class InitializationHolder {

        private static final OSFamily LOCAL_OS_FAMILY;

        static {
            // TODO review: is the fallback mechanism necessary and reliable? - misc_ro
            final String os = System.getProperty("os.name", "Linux" /* fallback */);
            if (os.startsWith("Windows")) {
                LOCAL_OS_FAMILY = Windows;
            } else if (os.toLowerCase().indexOf("linux") >= 0) {
                LOCAL_OS_FAMILY = OSFamily.Linux;
            } else {
                LOCAL_OS_FAMILY = Unspecified;
                LogFactory.getLog(OSFamily.class).warn("Local operating system family not recognized, setting to " + LOCAL_OS_FAMILY);
            }

        }
    }

    public static OSFamily getLocal() {
        return InitializationHolder.LOCAL_OS_FAMILY;
    }

    public static boolean isWindows() {
        return InitializationHolder.LOCAL_OS_FAMILY == Windows;
    }

    public static boolean isLinux() {
        return InitializationHolder.LOCAL_OS_FAMILY == Linux;
    }
}

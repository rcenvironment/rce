/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Class for detecting the runtime the code is executed in.
 *
 * @author Tobias Brieden
 */
public final class RuntimeDetection {

    private static final String ECLIPSE_APPLICATION_PROP = "eclipse.application";

    private static final String SUREFIRE_APPLICATION_IDENTIFIER = "org.eclipse.tycho.surefire.osgibooter.headlesstest";
    
    private RuntimeDetection() {}

    /**
     * @return True, if this code is executed by the surefire plugin or as a JUnit test.
     */
    public static boolean isRunningAsTest() {

        String applicationProperty = System.getProperty(ECLIPSE_APPLICATION_PROP);

        if (applicationProperty == null) { // TODO maybe this condition is too weak. Is there a better check possible?
            // in case of a JUnit test
            return true; 
        } else {
            return applicationProperty.equals(SUREFIRE_APPLICATION_IDENTIFIER);
        }
    }
}

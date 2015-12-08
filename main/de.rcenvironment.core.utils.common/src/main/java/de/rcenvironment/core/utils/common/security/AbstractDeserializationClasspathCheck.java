/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Provides an abstract base class checking for potential security issues related to deserialization of data received from external sources.
 * The abstract base class approach is used to ensure that the actual test runs in the classpath and bundle context of the caller, not the
 * one of a common utility class.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractDeserializationClasspathCheck {

    private static final String[] KNOWN_COMMONS_COLLECTIONS_ROOT_NAMESPACES = new String[] {
        "org.apache.commons.collections.",
        "org.apache.commons.collections4.",
        "org.apache.commons.collections15."
    };

    private static final String[] KNOWN_UNSAFE_OR_SUSPICIOUS_COMMONS_COLLECTIONS_CLASSES = new String[] {
        "Transformer",
        "Factory",
        "functors.ConstantTransformer",
        "functors.ConstantTransformer",
        "functors.ClosureTransformer",
        "functors.InvokerTransformer",
        "functors.InstantiateFactory",
        "functors.InstantiateTransformer",
        "functors.PrototypeFactory",
        // expand as necessary
    };

    private static final String[] OTHER_UNSAFE_OR_SUSPICIOUS_CLASSES = new String[] {
        "org.codehaus.groovy.runtime.ConvertedClosure",
        // TODO find out if this class is really affected
        // "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl",
        // expand as necessary
    };

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Checks the current classpath for classes known or suspected to be unsafe for deserialization of external data.
     * 
     * @return
     * 
     * @return true if problems were detected; false if all tests passed successfully
     */
    public boolean checkForKnownUnsafeClassesInClasspath() {

        // as this test checks for the ABSENCE of certain things, make sure the test patterns don't contain unintended characters
        final Pattern typoGuard = Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\.]+[a-zA-Z]$");
        final Bundle bundle = FrameworkUtil.getBundle(getClass());
        log.debug("Running in context of bundle " + bundle + " (may be 'null' when not running in an OSGi context)");

        boolean unsafeClassFound = false;

        for (String className : assembleListOfSuspiciousOrKnownUnsafeClassesForDeserialization()) {
            if (!typoGuard.matcher(className).matches()) {
                throw new IllegalArgumentException("The class name seems to be malformed: " + className);
            }
            try {
                Class.forName(className);
                log.error("Known unsafe class found in classpath: " + className);
                unsafeClassFound = true;
            } catch (ClassNotFoundException e) {
                try {
                    Thread.currentThread().getContextClassLoader().loadClass(className);
                    log.error("Known unsafe class found via context classloader: " + className);
                    unsafeClassFound = true;
                } catch (ClassNotFoundException e2) {
                    try {
                        if (bundle != null) {
                            bundle.loadClass(className);
                            log.error("Known unsafe class found via bundle classloader: " + className);
                            unsafeClassFound = true;
                        }
                    } catch (ClassNotFoundException e3) {
                        // ok
                        log.debug("Not found in classpath (good): " + className);
                    }
                }
            }
        }
        return unsafeClassFound;
    }

    private List<String> assembleListOfSuspiciousOrKnownUnsafeClassesForDeserialization() {

        List<String> list = new ArrayList<>();
        for (String prefix : KNOWN_COMMONS_COLLECTIONS_ROOT_NAMESPACES) {
            for (String suffix : KNOWN_UNSAFE_OR_SUSPICIOUS_COMMONS_COLLECTIONS_CLASSES) {
                list.add(prefix + suffix);
            }
        }
        for (String classname : OTHER_UNSAFE_OR_SUSPICIOUS_CLASSES) {
            list.add(classname);
        }

        return list;
    }
}

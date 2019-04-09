/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication.internal;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.AuthenticationTestConstants;
import de.rcenvironment.core.configuration.ConfigurationService;

/**
 * 
 * Mock factory for the authentication unit tests.
 *
 * @author Doreen Seider
 */
public final class AuthenticationMockFactory {
    
    /** Constant. */
    public static final String CA = System.getProperty("user.dir")
        + AuthenticationTestConstants.TESTRESOURCES_DIR + AuthenticationTestConstants.CA_FILE;

    /** Constant. */
    public static final String CRL = System.getProperty("user.dir")
        + AuthenticationTestConstants.TESTRESOURCES_DIR + AuthenticationTestConstants.CRL_FILE;
    
    /**
     * Constructor.
     */
    private AuthenticationMockFactory() {
    }
    
    /**
     * Getter.
     * @return The bundle context mock object.
     */
    public static BundleContext getBundleContextMock() {
        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn(AuthenticationTestConstants.BUNDLE_SYMBOLIC_NAME).anyTimes();
        EasyMock.replay(bundleMock);
        
        BundleContext bundleContextMock = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContextMock.getBundle()).andReturn(bundleMock).anyTimes();
        EasyMock.replay(bundleContextMock);
        return bundleContextMock;
    }

    /**
     * Getter.
     * @return The configuration service mock object.
     */
    public static ConfigurationService getConfigurationService() {

        AuthenticationConfiguration authenticationConfiguration = new AuthenticationConfiguration();
        
        List<String> caFiles = new ArrayList<String>();
        caFiles.add(CA);
        authenticationConfiguration.setCaFiles(caFiles);
        
        List<String> crlFiles = new ArrayList<String>();
        crlFiles.add(CRL);
        authenticationConfiguration.setCrlFiles(crlFiles);

        ConfigurationService configurationMock = EasyMock.createNiceMock(ConfigurationService.class);
        EasyMock.expect(configurationMock.getConfiguration(AuthenticationTestConstants.BUNDLE_SYMBOLIC_NAME,
            AuthenticationConfiguration.class)).andReturn(authenticationConfiguration).anyTimes();
        
        EasyMock.expect(configurationMock.resolveBundleConfigurationPath(AuthenticationTestConstants.BUNDLE_SYMBOLIC_NAME, CA))
            .andReturn(CA).anyTimes();
        EasyMock.expect(configurationMock.resolveBundleConfigurationPath(AuthenticationTestConstants.BUNDLE_SYMBOLIC_NAME, CRL))
            .andReturn(CRL).anyTimes();
        
        EasyMock.replay(configurationMock);

        return configurationMock;
    }
}

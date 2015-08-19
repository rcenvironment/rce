/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.xml.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Mock factory for the authorization unit tests.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (changed to classpath resource loading)
 */
public final class XMLAuthorizationMockFactory {

    /**
     * Constant.
     */
    public static final String AUTHORIZATION_XML_FULL_FILENAME;

    /**
     * Constant.
     */
    public static final String RELATIVE_XML_DOCUMENT = "authorization.xml";

    /**
     * Constant.
     */
    public static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.rce.authorization.xml";

    private static final String AUTHORIZATION_XML_RESOURCE_PATH = "/authorization.xml";

    /**
     * The bundle context mock.
     */
    private static BundleContext myBundleContextMock;

    static {
        InputStream stream = XMLAuthorizationMockFactory.class.getResourceAsStream(AUTHORIZATION_XML_RESOURCE_PATH);
        if (stream == null) {
            throw new RuntimeException("Failed to load classpath resource " + AUTHORIZATION_XML_RESOURCE_PATH);
        }
        try {
            File file = TempFileServiceAccess.getInstance().writeInputStreamToTempFile(stream);
            AUTHORIZATION_XML_FULL_FILENAME = file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * Constructor.
     * 
     */
    private XMLAuthorizationMockFactory() {}

    /**
     * Getter.
     * 
     * @return The configuration service mock object.
     */
    public static ConfigurationService getConfigurationService() {

        XMLAuthorizationConfiguration authorizationConfiguration = new XMLAuthorizationConfiguration();
        authorizationConfiguration.setXmlFile(AUTHORIZATION_XML_FULL_FILENAME);

        ConfigurationService configurationMock = EasyMock.createNiceMock(ConfigurationService.class);
        EasyMock.expect(configurationMock.getConfiguration(BUNDLE_SYMBOLIC_NAME,
            XMLAuthorizationConfiguration.class)).andReturn(authorizationConfiguration).anyTimes();

        EasyMock.expect(configurationMock.resolveBundleConfigurationPath(BUNDLE_SYMBOLIC_NAME, AUTHORIZATION_XML_FULL_FILENAME))
            .andReturn(AUTHORIZATION_XML_FULL_FILENAME).anyTimes();

        EasyMock.replay(configurationMock);

        return configurationMock;
    }

    /**
     * Getter.
     * 
     * @return The configuration service mock object.
     */
    public static ConfigurationService getAnotherConfigurationService() {

        XMLAuthorizationConfiguration authorizationConfiguration = new XMLAuthorizationConfiguration();
        authorizationConfiguration.setXmlFile(AUTHORIZATION_XML_FULL_FILENAME);

        ConfigurationService configurationMock = EasyMock.createNiceMock(ConfigurationService.class);
        EasyMock.expect(configurationMock.getConfiguration(BUNDLE_SYMBOLIC_NAME,
            XMLAuthorizationConfiguration.class)).andReturn(authorizationConfiguration).anyTimes();

        EasyMock.expect(configurationMock.resolveBundleConfigurationPath(BUNDLE_SYMBOLIC_NAME, AUTHORIZATION_XML_FULL_FILENAME))
            .andReturn("doesnotexist").anyTimes();

        EasyMock.replay(configurationMock);

        return configurationMock;
    }

    /**
     * 
     * Getter.
     * 
     * @return the bundle context mock object.
     * @throws InvalidSyntaxException if an error occurs.
     */
    public static BundleContext getBundleContextMock() throws InvalidSyntaxException {
        if (myBundleContextMock == null) {
            myBundleContextMock = createBundleContextMock(BUNDLE_SYMBOLIC_NAME);
        }
        return myBundleContextMock;
    }

    /**
     * 
     * Creates a bundle context mock.
     * 
     * @param bundleSymbolicName The symbolic name of the related bundle.
     * @return the bundle context mock.
     * @throws InvalidSyntaxException if an error occurs.
     */
    private static BundleContext createBundleContextMock(String bundleSymbolicName) throws InvalidSyntaxException {

        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andReturn(bundleSymbolicName).anyTimes();
        EasyMock.replay(bundle);

        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.replay(bundleContext);

        return bundleContext;
    }
}

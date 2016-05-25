/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.authorization.AuthorizationStore;
import de.rcenvironment.core.configuration.ConfigurationService;

/**
 * 
 * Mock factory for the authorization unit tests.
 *
 * @author Doreen Seider
 */
public final class AuthorizationMockFactory {

    /**
     * Constant.
     */
    private static final String EQUALS_SIGN = "=";

    /**
     * The bundle context mock.
     */
    private static BundleContext myBundleContextMock = null;

    /**
     * 
     * Constructor.
     * 
     */
    private AuthorizationMockFactory() {}

    /**
     * Getter.
     * 
     * @return The configuration service mock object.
     */
    public static ConfigurationService getConfigurationService() {

        AuthorizationConfiguration authorizationConfiguration = new AuthorizationConfiguration();
        authorizationConfiguration.setStore(AuthorizationStoreDummy.XML_STORE);

        ConfigurationService configurationMock = EasyMock.createNiceMock(ConfigurationService.class);
        EasyMock.expect(configurationMock.getConfiguration(AuthorizationStoreDummy.BUNDLE_SYMBOLIC_NAME,
            AuthorizationConfiguration.class)).andReturn(authorizationConfiguration).anyTimes();
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
            myBundleContextMock = createBundleContextMock(AuthorizationStoreDummy.BUNDLE_SYMBOLIC_NAME);
        }
        return myBundleContextMock;
    }

    /**
     * Dummy interface to ensure type safety for mocking.
     */
    private interface AuthorizationStoreDummyServiceReference extends ServiceReference<AuthorizationStoreDummy> {
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

        Bundle serviceCallProtocolBundleMock = createXMLAuthorizazionBundleMock();
        EasyMock.expect(bundleContext.getBundles()).andReturn(new Bundle[] { serviceCallProtocolBundleMock, bundle })
            .anyTimes();

        // RMI service call request sender factory reference
        ServiceReference<AuthorizationStoreDummy> storeReferenceMock =
            EasyMock.createNiceMock(AuthorizationStoreDummyServiceReference.class);
        // RMI service call request sender factory service
        String serviceProtocolFilter = "(" + AuthorizationStore.STORE + EQUALS_SIGN + AuthorizationStoreDummy.XML_STORE + ")";
        EasyMock.expect(bundleContext.getAllServiceReferences(AuthorizationStore.class.getName(), serviceProtocolFilter))
            .andReturn(new ServiceReference[] { storeReferenceMock }).anyTimes();

        EasyMock.expect(bundleContext.getService(storeReferenceMock)).andReturn(new AuthorizationStoreDummy()).anyTimes();

        EasyMock.replay(bundleContext);

        return bundleContext;
    }

    /**
     * 
     * Creates a service call protocol bundle mock.
     * 
     * @return The created mock.
     */
    private static Bundle createXMLAuthorizazionBundleMock() {
        Bundle protocolBundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(protocolBundleMock.getSymbolicName()).andReturn(AuthorizationStoreDummy.XML_STORE).anyTimes();
        EasyMock.replay(protocolBundleMock);
        return protocolBundleMock;
    }

}

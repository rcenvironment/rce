/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.mail.internal.MailConfiguration;

/**
 * Mock factory for the mail bundle's unit tests.
 * 
 * @author Tobias Menden
 */
public final class MailMockFactory {

    /**
     * Constant.
     */
    public static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.rce.mail";

    private static MailMockFactory instance;

    private BundleContext bundleContextMock;

    private MailMockFactory() {
        super();
    }

    /**
     * Getter.
     * 
     * @return the instance of this singleton object.
     */
    public static MailMockFactory getInstance() {
        if (instance == null) {
            instance = new MailMockFactory();
        }

        return instance;
    }

    /**
     * Getter.
     * 
     * @return the bundle context mock object.
     */
    public BundleContext getBundleContextMock() {
        if (bundleContextMock == null) {
            bundleContextMock = createBundleContextMock(BUNDLE_SYMBOLIC_NAME);
        }
        return bundleContextMock;
    }

    /**
     * Creates a configuration mock object.
     * 
     * @param useSSL flag indication whether ssl shall be used or not.
     * @param userPass set the user configuration.
     * @return the created settings mock object.
     */
    public ConfigurationService getConfigurationServiceMock(boolean useSSL, String userPass) {
        ConfigurationService configuration = EasyMock.createNiceMock(ConfigurationService.class);

        MailConfiguration mailConfiguration = new MailConfiguration();
        mailConfiguration.setMaillingLists(MailTestConstants.MAIL_ADDRESS_MAP);
        mailConfiguration.setUserPass(userPass);
        mailConfiguration.setSmtpServer("smtp.dlr.de");
        mailConfiguration.setUseSSL(useSSL);

        EasyMock.expect(configuration.getConfiguration(BUNDLE_SYMBOLIC_NAME, MailConfiguration.class))
            .andReturn(mailConfiguration).anyTimes();
        EasyMock.replay(configuration);

        return configuration;
    }

    /**
     * Creates a bundle context mock object.
     * 
     * @param bundleSymbolicName The symbolic name of the related bundle.
     * @return the bundle context mock.
     * @throws AuthenticationException Thrown on error.
     */
    private BundleContext createBundleContextMock(String bundleSymbolicName) {

        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME).anyTimes();
        EasyMock.replay(bundle);

        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.replay(bundleContext);

        return bundleContext;
    }

}

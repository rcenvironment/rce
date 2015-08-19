/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

import java.security.cert.X509Certificate;

import org.easymock.EasyMock;
import org.globus.gsi.OpenSSLKey;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.authentication.AuthenticationService.X509AuthenticationResult;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.notification.NotificationService;

/**
 * 
 * Mock factory for the login unit tests.
 * 
 * @author Doreen Seider
 * @author Tobias Menden
 */
public final class LoginMockFactory {

    /**
     * Constant.
     */
    public static final String PASSWORD = "password";

    /**
     * Constant.
     */
    public static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.rce.login";

    /**
     * Constant.
     */
    public static final int VALIDITY_IN_DAYS = 7;

    /**
     * The instance of this singleton class.
     */
    private static LoginMockFactory instance;

    /**
     * The bundle context mock.
     */
    private BundleContext bundleContextMock;

    /**
     * The broken bundle context mock.
     */
    private BundleContext brokenBundleContextMock;

    /**
     * Constructor.
     */
    private LoginMockFactory() {
        super();
    }

    /**
     * Getter.
     * 
     * @return the instance of this singleton object.
     */
    public static LoginMockFactory getInstance() {
        if (instance == null) {
            instance = new LoginMockFactory();
        }

        return instance;
    }

    /**
     * Getter.
     * 
     * @return the bundle context mock object.
     * @throws AuthenticationException Thrown on error.
     */
    public BundleContext getBundleContextMock() throws AuthenticationException {
        if (bundleContextMock == null) {
            bundleContextMock = createBundleContextMock(BUNDLE_SYMBOLIC_NAME);
        }
        return bundleContextMock;
    }

    /**
     * Getter.
     * 
     * @return the broken bundle context mock object.
     */
    public BundleContext getBrokenBundleContextMock() {
        if (brokenBundleContextMock == null) {
            brokenBundleContextMock = createBrokenBundleContextMock();
        }
        return brokenBundleContextMock;
    }

    /**
     * Creates a configuration mock object.
     * 
     * @return the created settings mock object.
     */
    public ConfigurationService getConfigurationServiceMock() {

        ConfigurationService configuration = EasyMock.createNiceMock(ConfigurationService.class);

        LoginConfiguration loginConfiguration = new LoginConfiguration();
        loginConfiguration.setAutoLogin(false);
        loginConfiguration.setCertificateFile(LoginTestConstants.USER_1_CERTIFICATE_FILENAME);
        loginConfiguration.setKeyFile(LoginTestConstants.USER_1_KEY_FILENAME);
        loginConfiguration.setAutoLoginPassword("test");

        EasyMock.expect(configuration.getConfiguration(BUNDLE_SYMBOLIC_NAME, LoginConfiguration.class))
            .andReturn(loginConfiguration).anyTimes();

        EasyMock.expect(configuration.resolveBundleConfigurationPath(BUNDLE_SYMBOLIC_NAME, LoginTestConstants.USER_1_CERTIFICATE_FILENAME))
            .andReturn(LoginTestConstants.USER_1_CERTIFICATE_FILENAME).anyTimes();
        EasyMock.expect(configuration.resolveBundleConfigurationPath(BUNDLE_SYMBOLIC_NAME, LoginTestConstants.USER_1_KEY_FILENAME))
            .andReturn(LoginTestConstants.USER_1_KEY_FILENAME).anyTimes();

        EasyMock.replay(configuration);

        return configuration;
    }

    /**
     * Creates a notification mock object.
     * 
     * @return the created settings mock object.
     */
    public NotificationService getNotificationServiceMock() {

        NotificationService notification = EasyMock.createNiceMock(NotificationService.class);
        EasyMock.replay(notification);

        return notification;
    }

    /**
     * Creates a settings mock object.
     * 
     * @return the created settings mock object.
     * @throws AuthenticationException if an error occurs.
     */
    public AuthenticationService getAuthenticationServiceMock() throws AuthenticationException {

        AuthenticationService authentication = EasyMock.createNiceMock(AuthenticationService.class);

        LoginInput loginInput = LoginInputFactory.getLoginInputForCertificate();
        X509Certificate certificate = loginInput.getCertificate();
        OpenSSLKey key = loginInput.getKey();
        String password = loginInput.getPassword();

        LoginInput antotherLoginInput = LoginInputFactory.getAnotherLoginInputForCertificate();
        X509Certificate antotherCertificate = antotherLoginInput.getCertificate();

        EasyMock.expect(authentication.authenticate(null, null, null)).andThrow(new AuthenticationException("")).anyTimes();

        EasyMock.expect(authentication.authenticate(certificate, key, password)).andReturn(X509AuthenticationResult.CERTIFICATE_REVOKED);
        EasyMock.expect(authentication.authenticate(certificate, key, password))
            .andReturn(X509AuthenticationResult.NOT_SIGNED_BY_TRUSTED_CA);
        EasyMock.expect(authentication.authenticate(certificate, key, password)).andReturn(X509AuthenticationResult.PASSWORD_INCORRECT);
        EasyMock.expect(authentication.authenticate(certificate, key, password)).andReturn(X509AuthenticationResult.PASSWORD_REQUIRED);
        EasyMock.expect(authentication.authenticate(certificate, key, password)).
            andReturn(X509AuthenticationResult.PRIVATE_KEY_NOT_BELONGS_TO_PUBLIC_KEY);
        EasyMock.expect(authentication.authenticate(certificate, key, password)).andReturn(X509AuthenticationResult.AUTHENTICATED);
        EasyMock.expect(authentication.authenticate(certificate, key, password)).andReturn(X509AuthenticationResult.AUTHENTICATED);

        EasyMock.expect(authentication.createUser(LoginTestConstants.USER_1_CERTIFICATE, VALIDITY_IN_DAYS))
            .andReturn(new DummyUser(VALIDITY_IN_DAYS)).anyTimes();
        EasyMock.expect(authentication.createUser(LoginTestConstants.USER_2_CERTIFICATE, VALIDITY_IN_DAYS))
            .andReturn(new DummyUser(VALIDITY_IN_DAYS)).anyTimes();

        EasyMock.expect(authentication.loadCertificate(LoginTestConstants.USER_1_CERTIFICATE_FILENAME))
            .andReturn(certificate).anyTimes();
        EasyMock.expect(authentication.loadKey(LoginTestConstants.USER_1_KEY_FILENAME))
            .andReturn(key).anyTimes();

        // LDAP
        EasyMock.expect(authentication.authenticate("a", "b")).
            andReturn(AuthenticationService.LDAPAuthenticationResult.AUTHENTICATED).anyTimes();
        EasyMock.expect(authentication.authenticate("a", "c")).
            andReturn(AuthenticationService.LDAPAuthenticationResult.PASSWORD_OR_USERNAME_INCORRECT).anyTimes();

        EasyMock.replay(authentication);

        return authentication;
    }

    /**
     * Creates a broken bundle context mock.
     * 
     * @return the broken context mock.
     */
    private BundleContext createBrokenBundleContextMock() {

        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getServiceReference(ConfigurationService.class.getName())).andReturn(null).anyTimes();
        EasyMock.replay(bundleContext);

        return bundleContext;
    }

    /**
     * Creates a bundle context mock.
     * 
     * @param bundleSymbolicName The symbolic name of the related bundle.
     * @return the bundle context mock.
     * @throws AuthenticationException Thrown on error.
     */
    private BundleContext createBundleContextMock(String bundleSymbolicName) throws AuthenticationException {

        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME).anyTimes();
        EasyMock.replay(bundle);

        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.replay(bundleContext);

        return bundleContext;
    }

}

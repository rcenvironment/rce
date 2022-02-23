/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authentication.internal;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.authentication.LDAPUser;
import de.rcenvironment.core.authentication.SingleUser;
import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Implementation of <code>AuthenticationService</code> interface.
 * 
 * @author Doreen Seider
 * @author Tobias Menden
 * @author Alice Zorn
 */
public class AuthenticationServiceImpl implements AuthenticationService {

    /* For LDAP authentication */
    /**
     * Constant that holds the name of the environment property for specifying how referrals encountered by the service provider are to be
     * processed. The value of the property is one of the following strings: "follow": follow referrals automatically "ignore": ignore
     * referrals "throw": throw ReferralException when a referral is encountered.
     */
    private static final String REFERRAL = "follow";

    /** Context Factory class. */
    private static final String CONTEXT_FACTORY_CLASS = "com.sun.jndi.ldap.LdapCtxFactory";

    /** LDAP authentification method. */
    private static final String LDAP_AUTH_METHOD = "simple";

    /** LDAP protocol. */
    private static final String LDAP_PROTOCOL = "ldap://";

    private static final String ASSERTIONS_PARAMETER_NULL = "The parameter \"%s\" must not be null.";

    private static final Log LOGGER = LogFactory.getLog(AuthenticationServiceImpl.class);

    private AuthenticationConfiguration myConfiguration;

    private ConfigurationService configurationService;

    private String bundleSymbolicName;

    protected void activate(BundleContext context) {
        bundleSymbolicName = context.getBundle().getSymbolicName();
        // note: disabled old configuration loading for 6.0.0 as it is not being used anyway
        // myConfiguration = configurationService.getConfiguration(bundleSymbolicName, AuthenticationConfiguration.class);
        // TODO using default values until reworked or removed
        myConfiguration = new AuthenticationConfiguration();
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    @Override
    public LDAPAuthenticationResult authenticate(String username, String password) {

        if (password == null || password.trim().isEmpty() 
            || username == null || username.trim().isEmpty()) {
            return LDAPAuthenticationResult.PASSWORD__OR_USERNAME_INVALID;
        }

        String baseDn = myConfiguration.getLdapBaseDn();
        String server = myConfiguration.getLdapServer();
        String domain = myConfiguration.getLdapDomain();

        try {
            connect(server, baseDn, username + "@" + domain, password);
        } catch (NamingException e) {
            return LDAPAuthenticationResult.PASSWORD_OR_USERNAME_INCORRECT;
        }

        return LDAPAuthenticationResult.AUTHENTICATED;
    }

    @Override
    public User createUser(String userIdLdap, int validityInDays) {
        Assertions.isDefined(userIdLdap, ASSERTIONS_PARAMETER_NULL);
        return new LDAPUser(userIdLdap, validityInDays, myConfiguration.getLdapDomain());
    }

    @Override
    public User createUser(int validityInDays) {
        return new SingleUser(validityInDays);
    }

    /**
     * 
     * Tries to set up and bind the LDAP-Connection and sets dirContext to an initialized directory service. Reads properties from the file
     * ldap.properties and sets the context.
     * 
     * @param server the ldap server
     * @param baseDn the ldap base dn
     * @param dn the ldap dn
     * @param password the ldap password
     * @throws NamingException if the input was a wrong password or username
     * 
     */
    private void connect(String server, String baseDn, String dn, String password) throws NamingException {

        Properties env = new Properties();
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY_CLASS);
        env.setProperty(Context.PROVIDER_URL, LDAP_PROTOCOL + server);
        env.setProperty(Context.SECURITY_AUTHENTICATION, LDAP_AUTH_METHOD);
        env.setProperty(Context.SECURITY_PRINCIPAL, dn);
        env.setProperty(Context.SECURITY_CREDENTIALS, password);
        env.setProperty(Context.REFERRAL, REFERRAL);

        // If a username or password does not exists a NamingException is thrown.
        new InitialDirContext(env);
    }


}

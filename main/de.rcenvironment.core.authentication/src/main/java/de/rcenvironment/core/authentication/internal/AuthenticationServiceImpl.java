/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication.internal;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.gsi.CertUtil;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.authentication.CertificateUser;
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

    private static final String ASSERTIONS_PARAMETER_NULL = "The parameter \"{0}\" must not be null.";

    private static final String DSA = "DSA";

    private static final String RSA = "RSA";

    private static final Log LOGGER = LogFactory.getLog(AuthenticationServiceImpl.class);

    private static final String CERTIFICATE_COULD_NOT_BE_LOADED = "The given CA certificate (%s) could not be loaded.";

    private static final String CRL_COULD_NOT_BE_LOADED = "The given certificate revocation list (CRL) (%s) could not be loaded.";

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
    @Deprecated
    // note: some unit tests are already ignored due to maintenance effort for required test infrastructure
    public X509AuthenticationResult authenticate(X509Certificate certificate, OpenSSLKey encryptedKey, String password)
        throws AuthenticationException {

        Assertions.isDefined(certificate, MessageFormat.format(ASSERTIONS_PARAMETER_NULL, "certificate"));
        Assertions.isDefined(encryptedKey, MessageFormat.format(ASSERTIONS_PARAMETER_NULL, "key"));

        X509AuthenticationResult result = null;

        try {
            certificate.checkValidity();
        } catch (CertificateNotYetValidException e) {
            throw new AuthenticationException(e);
        } catch (CertificateExpiredException e) {
            throw new AuthenticationException(e);
        }

        if (password == null && isPasswordNeeded(encryptedKey)) {
            result = X509AuthenticationResult.PASSWORD_REQUIRED;
        }

        if (result == null && !isPasswordCorrect(encryptedKey, password)) {
            result = X509AuthenticationResult.PASSWORD_INCORRECT;
        }

        if (result == null && !isPrivateKeyBelongingToCertificate(certificate, encryptedKey)) {
            result = X509AuthenticationResult.PRIVATE_KEY_NOT_BELONGS_TO_PUBLIC_KEY;
        }

        if (result == null && !isSignedByTrustedCA(certificate)) {
            result = X509AuthenticationResult.NOT_SIGNED_BY_TRUSTED_CA;
        }

        if (result == null && isRevoced(certificate)) {
            result = X509AuthenticationResult.CERTIFICATE_REVOKED;
        }

        if (result == null) {
            result = X509AuthenticationResult.AUTHENTICATED;
        }

        return result;
    }

    @Override
    public LDAPAuthenticationResult authenticate(String username, String password) {

        LDAPAuthenticationResult result = null;

        if (password == null || password.trim().equals("")) {
            result = LDAPAuthenticationResult.PASSWORD_INVALID;
        }

        String baseDn = myConfiguration.getLdapBaseDn();
        String server = myConfiguration.getLdapServer();
        String domain = myConfiguration.getLdapDomain();

        try {
            connect(server, baseDn, username + "@" + domain, password);
        } catch (NamingException e) {
            result = LDAPAuthenticationResult.PASSWORD_OR_USERNAME_INCORRECT;
        }

        if (result == null) {
            result = LDAPAuthenticationResult.AUTHENTICATED;
        }

        return result;
    }

    @Override
    public User createUser(X509Certificate certificate, int validityInDays) {
        Assertions.isDefined(certificate, ASSERTIONS_PARAMETER_NULL);
        return new CertificateUser(certificate, validityInDays);
    }

    @Override
    public User createUser(String userIdLdap, int validityInDays) {
        Assertions.isDefined(userIdLdap, ASSERTIONS_PARAMETER_NULL);
        return new LDAPUser(userIdLdap, validityInDays, myConfiguration.getLdapDomain());
    }

    @Override
    public X509Certificate loadCertificate(String file) throws AuthenticationException {

        Assertions.isDefined(file, "The parameter 'file' (path to the certificate) must not be null.");

        try {
            return CertUtil.loadCertificate(file);
        } catch (IOException e) {
            throw new AuthenticationException(e);
        } catch (GeneralSecurityException e) {
            throw new AuthenticationException(e);
        }
    }

    @Override
    public OpenSSLKey loadKey(String file) throws AuthenticationException {

        Assertions.isDefined(file, "The parameter 'file' (path to the key) must not be null.");

        try {
            return new BouncyCastleOpenSSLKey(file);
        } catch (IOException e) {
            throw new AuthenticationException(e);
        } catch (GeneralSecurityException e) {
            throw new AuthenticationException(e);
        }
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

    /**
     * Checks if a password is required.
     * 
     * @param encryptedKey The private key to decrypt.
     * @return true if password is needed, else false.
     */
    private boolean isPasswordNeeded(OpenSSLKey encryptedKey) {

        return encryptedKey.isEncrypted();
    }

    /**
     * Checks if the password to decrypt the private key is correct.
     * 
     * @param encryptedKey The private key to decrypt.
     * @param password The password for decrypting.
     * @return true if password is correct, else false.
     * @throws AuthenticationException if an error during decrypting occurs.
     */
    private boolean isPasswordCorrect(OpenSSLKey encryptedKey, String password) throws AuthenticationException {

        boolean correct = true;

        // validate the password by decrypting the private key with the password
        if (encryptedKey.isEncrypted()) {
            Assertions.isDefined(password, "If the key is encrypted the password must not be null.");
            try {
                encryptedKey.decrypt(password);
            } catch (BadPaddingException e) {
                correct = false;
            } catch (RuntimeException e) {
                throw new AuthenticationException(e);
            } catch (InvalidKeyException e) {
                throw new AuthenticationException(e);
            } catch (GeneralSecurityException e) {
                throw new AuthenticationException(e);
            }
        }
        return correct;
    }

    /**
     * Checks if the given private key belongs to the given public key (certificate).
     * 
     * @param certificate The public key (certificate).
     * @param encryptedKey The private key.
     * @return true if the private key belongs to the certificate, else false.
     * @throws AuthenticationException if an exception during decrypting/encrypting occurs.
     */
    private boolean isPrivateKeyBelongingToCertificate(X509Certificate certificate, OpenSSLKey encryptedKey)
        throws AuthenticationException {

        boolean belongs = true;

        PrivateKey privateKey = encryptedKey.getPrivateKey();
        PublicKey publicKey = certificate.getPublicKey();

        Random random = new Random();
        String original = Long.toString(Math.abs(random.nextLong()));

        // encrypt (with the private key) and decrypt (with the public key) a
        // random string
        try {
            Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] encrypted = cipher.doFinal(original.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] decrypted = cipher.doFinal(encrypted);

            if (!original.equals(new String(decrypted))) {
                belongs = false;
            }
        } catch (BadPaddingException e) {
            belongs = false;
        } catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(e);
        } catch (NoSuchPaddingException e) {
            throw new AuthenticationException(e);
        } catch (InvalidKeyException e) {
            throw new AuthenticationException(e);
        } catch (IllegalBlockSizeException e) {
            throw new AuthenticationException(e);
        }
        return belongs;
    }

    /**
     * Checks if the certificate is signed by a trusted CA.
     * 
     * @param certificate The certificate to validate.
     * @return true if it is signed by a trusted CA, else false.
     * @throws AuthenticationException if an an error occurs during verification.
     */
    private boolean isSignedByTrustedCA(X509Certificate certificate) throws AuthenticationException {

        boolean signed = false;

        // add the given certificates to the context
        for (X509Certificate caCertificate : getCertificateAuthorities()) {

            try {
                caCertificate.checkValidity();
            } catch (CertificateNotYetValidException e) {
                throw new AuthenticationException(e);
            } catch (CertificateExpiredException e) {
                throw new AuthenticationException(e);
            }

            // if the CA signed the certificate
            if (certificate.getIssuerDN().equals(caCertificate.getSubjectDN())) {

                // try to encrypt the signature of the certificate with the
                // public key
                // of the CA
                String encryptionAlgorithm = null;
                if (certificate.getSigAlgName().contains(RSA)) {
                    encryptionAlgorithm = RSA;
                } else if (certificate.getSigAlgName().contains(DSA)) {
                    encryptionAlgorithm = DSA;
                } else {
                    throw new AuthenticationException("The encryption algorithm of the certificates's signature is not supported.");
                }

                try {
                    Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
                    cipher.init(Cipher.DECRYPT_MODE, caCertificate.getPublicKey());
                    cipher.doFinal(certificate.getSignature());
                    signed = true;
                } catch (InvalidKeyException e) {
                    throw new AuthenticationException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new AuthenticationException(e);
                } catch (NoSuchPaddingException e) {
                    throw new AuthenticationException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new AuthenticationException(e);
                } catch (BadPaddingException e) {
                    throw new AuthenticationException(e);
                }
            }
        }
        return signed;
    }

    /**
     * Checks if the certificate is revoked from its CA.
     * 
     * @param certificate The certificate to check.
     * @return false if the certificate is not revoked, else true;
     * @throws AuthenticationException if an error during checking for revocation occurs.
     */
    private boolean isRevoced(X509Certificate certificate) throws AuthenticationException {

        boolean revoked = false;

        // read the given revocation lists
        for (X509CRL revocationList : getCertificateRevocationLists()) {

            Date now = new Date();
            // is the CRL of the CA signing the certificate
            if (revocationList.getIssuerDN().equals(certificate.getIssuerDN())) {
                // is the CRL not expired
                if (revocationList.getThisUpdate().before(now)
                    && (revocationList.getNextUpdate() == null || revocationList.getNextUpdate().after(now))) {

                    if (revocationList.isRevoked(certificate)) {
                        revoked = true;
                    }
                } else {
                    throw new AuthenticationException("The CRL of the CA is not valid (e.g., it is expired).");
                }
            }
        }

        return revoked;
    }

    /**
     * 
     * Returns the certificate authority certificate paths as a comma separated string.
     * 
     * @return comma separated string with CRL paths.
     */
    private List<X509Certificate> getCertificateAuthorities() {

        List<X509Certificate> certificates = new Vector<X509Certificate>();

        String absPath = null;
        for (String path : myConfiguration.getCaFiles()) {
            try {
                absPath = configurationService.resolveBundleConfigurationPath(bundleSymbolicName, path);
                certificates.add(CertUtil.loadCertificate(absPath));
            } catch (IOException e) {
                LOGGER.error(String.format(CERTIFICATE_COULD_NOT_BE_LOADED, absPath));
            } catch (GeneralSecurityException e) {
                LOGGER.error(String.format(CERTIFICATE_COULD_NOT_BE_LOADED, absPath));
            }
        }
        return certificates;
    }

    /**
     * 
     * Returns the certificate revocation list paths as a comma separated string.
     * 
     * @return comma separated string with CA paths.
     */
    private List<X509CRL> getCertificateRevocationLists() {

        List<X509CRL> certificateRevocationLists = new Vector<X509CRL>();

        String absPath = null;
        for (String path : myConfiguration.getCrlFiles()) {
            try {
                absPath = configurationService.resolveBundleConfigurationPath(bundleSymbolicName, path);
                certificateRevocationLists.add(CertUtil.loadCrl(absPath));
            } catch (IOException e) {
                LOGGER.error(String.format(CRL_COULD_NOT_BE_LOADED, absPath));
            } catch (GeneralSecurityException e) {
                LOGGER.error(String.format(CRL_COULD_NOT_BE_LOADED, absPath));
            }
        }

        return certificateRevocationLists;
    }

}

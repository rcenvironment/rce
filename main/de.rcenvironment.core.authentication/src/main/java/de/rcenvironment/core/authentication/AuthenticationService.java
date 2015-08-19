/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication;

import java.security.cert.X509Certificate;

import org.globus.gsi.OpenSSLKey;


/**
 * Interface for the authentication service.
 * 
 * @author Doreen Seider
 * @author Alice Zorn
 */
public interface AuthenticationService {

    /**
     * Result types.
     *
     * @author Doreen Seider
     */
    enum X509AuthenticationResult {
        
        /**
         * The certificate is revoked by its certificate authority.
         */
        PASSWORD_REQUIRED,
        /**
         * The certificate is revoked by its certificate authority.
         */
        CERTIFICATE_REVOKED,
        /**
         * The password for decrypting the private key is incorrect.
         */
        PASSWORD_INCORRECT,
        /**
         * The certificate is not signed by a trusted certificate authority.
         */
        NOT_SIGNED_BY_TRUSTED_CA,
        /**
         * The private key does not belong to the public one.
         */
        PRIVATE_KEY_NOT_BELONGS_TO_PUBLIC_KEY,
        /**
         * The authentication was successful.
         */
        AUTHENTICATED
    }
    
    /**
     * 
     * Result types for LDAP authentication.
     *
     * @author Alice Zorn
     */
    enum LDAPAuthenticationResult {
        
        /**
         * The password for decrypting the private key is empty or null.
         */
        PASSWORD_INVALID,
        /**
         * The password for decrypting the private key is incorrect.
         */
        PASSWORD_OR_USERNAME_INCORRECT,
        /**
         * The authentication was successful.
         */
        AUTHENTICATED
    }
    
    /**
     * Authenticates with the given certificate, key and password.
     * 
     * @param certificate The given certificate. 
     * @param encryptedKey The given encrypted private key.
     * @param password The given password.
     * @return The result of the authentication.
     * @throws AuthenticationException if an error occurs during the authentication process.
     */
    @Deprecated // note: some unit tests are already ignored due to maintenance effort for required test infrastructure
    X509AuthenticationResult authenticate(X509Certificate certificate, OpenSSLKey encryptedKey, String password)
        throws AuthenticationException;
    
    
    /**
     * Authenticates with the given username and password.
     * 
     * @param uid The user ID
     * @param password The given password.
     * @return The result of the authentication.
     */
    LDAPAuthenticationResult authenticate(String uid, String password);

    
    /**
     * 
     * Loads a certificate (public key) from a file.
     * 
     * @param file The file (path to it) of the certificate.
     * @return The loaded certificate as {@link X509Certificate} object.
     * @throws AuthenticationException if an error occurs during loading the certificate.
     */
    X509Certificate loadCertificate(String file) throws AuthenticationException;

    /**
     * 
     * Loads a key (private key) from file.
     * 
     * @param file The file (path to it) of the key.
     * @return The loaded key as {@link OpenSSLKey} object.
     * @throws AuthenticationException if an error occurs during loading the key.
     */
    OpenSSLKey loadKey(String file) throws AuthenticationException;
    
    /**
     * 
     * Creates a {@link User}.
     * 
     * @param certificate user's certificate.
     * @param validityInDays the user's validity in days
     * @return the {@link User}.
     */
    User createUser(X509Certificate certificate, int validityInDays);

    /**
     * Returns a {@link User} object with the default validity.
     * 
     * @param userIdLdap The user's ID.
     * @param validityInDays the user's validity in days
     * @return the {@link User}.
     */
    User createUser(String userIdLdap, int validityInDays);
    
    /**
     * Returns a {@link User} object with the default validity.
     * 
     * @param validityInDays the user's validity in days
     * @return the {@link User}.
     */
    User createUser(int validityInDays);

}

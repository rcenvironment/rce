/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authentication;

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
        PASSWORD__OR_USERNAME_INVALID,
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
     * Authenticates with the given username and password.
     * 
     * @param uid The user ID
     * @param password The given password.
     * @return The result of the authentication.
     */
    LDAPAuthenticationResult authenticate(String uid, String password);

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

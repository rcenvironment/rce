/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

import java.security.cert.X509Certificate;

import org.globus.gsi.OpenSSLKey;

import de.rcenvironment.core.authentication.User.Type;
import de.rcenvironment.core.login.internal.Messages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Data object encapsulating all informations needed for authentication.
 * 
 * @author Doreen Seider
 * @author Alice Zorn
 */
public class LoginInput {

    private static final String ASSERTIONS_PARAMETER_NULL = Messages.assertionsParameterNull;

    private static final String ASSERTIONS_STRING_EMPTY = Messages.assertionsStringEmpty;

    /**
     * The certificate (public key).
     */
    private X509Certificate certificate;

    /**
     * The key (private key).
     */
    private OpenSSLKey key;

    /**
     * The password the is encrypted with.
     */
    private String password;
    
    /**
     * The username for the LDAP login.
     */
    private String usernameLDAP;
    
    
    /** The login type. */
    private final Type type;
    

    /**
     * Constructor.
     * @param certificate The certificate (public key).
     * @param key The key (private key).
     * @param password The password the private key is encrypted with.
     */
    public LoginInput(X509Certificate certificate, OpenSSLKey key, String password) {
        Assertions.isDefined(certificate, StringUtils.format(ASSERTIONS_PARAMETER_NULL, Messages.certificate));
        Assertions.isDefined(key, StringUtils.format(ASSERTIONS_PARAMETER_NULL, Messages.key));

        type = Type.certificate;
        this.certificate = certificate;
        this.key = key;
        this.password = password;
    }
    
    /**
     * Constructor.
     * @param usernameLDAP The username for the LDAP login.
     * @param password The password the private key is encrypted with.
     */
    public LoginInput(final String usernameLDAP, final String password) {
        Assertions.isDefined(usernameLDAP, StringUtils.format(ASSERTIONS_PARAMETER_NULL, Messages.usernameLDAP));
        Assertions.isDefined(password, StringUtils.format(ASSERTIONS_PARAMETER_NULL, Messages.password));
        Assertions.isTrue(usernameLDAP.length() > 0, StringUtils.format(ASSERTIONS_STRING_EMPTY, Messages.usernameLDAP));
        
        type = Type.ldap;
        this.usernameLDAP = usernameLDAP;
        this.password = password;
    }

    public LoginInput(boolean anonymousLogin) {
        type = Type.single;
    }

    /**
     * Getter.
     * 
     * @return The certificate (public key).
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * Getter.
     * 
     * @return The key (private key).
     */
    public OpenSSLKey getKey() {
        return key;
    }

    /**
     * 
     * Getter.
     * 
     * @return The password the is encrypted with.
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * 
     * Getter.
     * 
     * @return The username for the LDAP login.
     */
    public String getUsernameLDAP(){
        return usernameLDAP;
    }
    
    /**
     * Getter.
     * 
     * @return the type of the login: LDAP or Certificate
     */
    public Type getType(){
        return type;
    }
    

}

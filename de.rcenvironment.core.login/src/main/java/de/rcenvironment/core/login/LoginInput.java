/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

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

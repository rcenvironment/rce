/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.authentication;

/**
 * LDAP based extension of {@link User}.
 * (needs to be exported because it needs to be visible for communication bundle in order
 * to be able to deserialize objects of this class)
 * 
 * @author Alice Zorn
 */
public class LDAPUser extends User {

    private static final long serialVersionUID = -578294853143565416L;
    
    private static final Type TYPE = Type.ldap;

    private final String userId;
    
    private String domain = "dlr";
    
    public LDAPUser(String userId, int validityInDays, String domain){
        super(validityInDays);
        this.userId = userId;
        this.domain = domain;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public String getDomain() {
        return domain;
    }

}

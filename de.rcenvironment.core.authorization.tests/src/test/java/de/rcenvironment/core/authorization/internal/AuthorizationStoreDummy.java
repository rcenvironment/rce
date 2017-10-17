/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.authorization.AuthorizationStore;
import de.rcenvironment.core.authorization.AuthorizationStoreException;
import de.rcenvironment.core.authorization.rbac.Permission;
import de.rcenvironment.core.authorization.rbac.Role;
import de.rcenvironment.core.authorization.rbac.Subject;

/**
 * 
 * Dummy for an authorization store for test issues.
 * 
 * @author Doreen Seider
 */
public class AuthorizationStoreDummy implements AuthorizationStore {
    
    /**
     * Constant.
     */
    public static final String XML_STORE = "de.rcenvironment.rce.authorization.xml";
    
    /**
     * Constant.
     */
    public static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.rce.authorization";
        
    /**
     * Test permission ID.
     */
    public static final String PERMISSION_ID = "permissionID";

    /**
     * Test permission.
     */
    public static final Permission PERMISSION;
    
    /**
     * Test permission set.
     */
    public static final Set<Permission> PERMISSION_SET;
    
    /**
     * Test role ID.
     */
    public static final String ROLE_ID = "roleID";

    /**
     * Test role.
     */
    public static final Role ROLE;
    
    /**
     * Test role set.
     */
    public static final Set<Role> ROLE_SET;
    
    /**
     * Test subject ID.
     */
    public static final String SUBJECT_ID = "subjectID";

    /**
     * Test subject.
     */
    public static final Subject SUBJECT;
    
    /**
     * 
     * Fills the sets.
     *
     */
    static {
        PERMISSION = new Permission(PERMISSION_ID);
        PERMISSION_SET = new HashSet<Permission>();
        PERMISSION_SET.add(PERMISSION);

        ROLE = new Role(ROLE_ID, PERMISSION_SET);
        ROLE_SET = new HashSet<Role>();
        ROLE_SET.add(ROLE);
        
        SUBJECT = new Subject(SUBJECT_ID, ROLE_SET);
    }

    @Override
    public void initialize() throws AuthorizationStoreException {

    }

    @Override
    public Permission lookupPermission(String permissionID) {
        return PERMISSION;
    }

    @Override
    public Role lookupRole(String roleID) {
        return ROLE;
    }

    @Override
    public Subject lookupSubject(String subjectID) {
        return SUBJECT;
    }

}

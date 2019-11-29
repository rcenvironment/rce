/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authentication;

import de.rcenvironment.core.authentication.internal.AuthenticationConfiguration;

/**
 * Class representing a user's interactive session.
 * 
 * @author Thijs Metsch
 * @author Andre Nurzenski
 * @author Andreas Baecker
 * @author Doreen Seider
 * @author Heinrich Wendel
 * @author Alice Zorn 
 */
public final class Session {

    /** Identifier of the notification sent for a new user login. */
    public static final String NOTIFICATION_ID_NEWUSER = "de.rcenvironment.rce.authentication.newuser"; //$NON-NLS-1$
    
    private static Session instance = null;

    private static User user = null;
    
    private Session(User user) {
        Session.user = user;
        Session.instance = this;
    }
    
    /**
     * Returns whether a {@link Session} is initialized.
     * 
     * @return true, if a {@link Session} is initialized
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Returns an instance of this class.
     * 
     * @return The instance of this class.
     * @throws AuthenticationException Thrown if user not logged in.
     */
    public static Session getInstance() throws AuthenticationException {
        if (instance != null) {
            return instance;
        } else {
            throw new AuthenticationException("There is no session initialized.");
        }
    }

    public User getUser() {
        return user;
    }
    
    /**
     * Creates a {@link Session}.
     * 
     * @param newUser The new User.
     * 
     */
    public static synchronized void create(User newUser) {
        if (instance == null) {
            new Session(newUser);            
        } else {
            Session.user = newUser;
        }
    }
    
    /**
     * Creates a {@link Session}.
     * 
     * @param userID The User's ID
     * @param validityInDays The validity of the user in days.
     * 
     */
    public static synchronized void create(String userID, int validityInDays) {
        AuthenticationConfiguration config = new AuthenticationConfiguration();
        LDAPUser newUser = new LDAPUser(userID, validityInDays, config.getLdapDomain());

        if (instance == null) {
            new Session(newUser);            
        } else {
            Session.user = newUser;
        }
    }
    
    /**
     * Destroys the current {@link Session}.
     */
    public synchronized void destroy() {
        instance = null;
        Session.user = null;
    }
}

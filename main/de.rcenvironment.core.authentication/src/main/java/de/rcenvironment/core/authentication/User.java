/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Represents a user authenticated by the system. This representation is valid for a limited specified time.
 * 
 * @author Andre Nurzenski
 * @author Alice Zorn
 * @author Doreen Seider
 */
public abstract class User implements Serializable {
    
    /**
     * The type of the users.
     *
     * @author Alice Zorn
     */
    public enum Type{
        
        /**
         * the single-mode user.
         */
        single,
        /**
         * The certificate user.
         */
        certificate,
        
        /**
         * the LDAP user.
         */
        ldap;
    }

    private static final long serialVersionUID = 8062621616567258257L;
    
    private final Date timeUntilValid;
    
    private int validityInDays;
        
    public User(int validityInDays) {
        if (validityInDays <= 0){
            throw new IllegalArgumentException();
        }
        this.validityInDays = validityInDays;
        
        // calculate the date until the user representation is valid 
        final Date now = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_MONTH, validityInDays);
        timeUntilValid = cal.getTime();
    }
    
    /**
     * Checks if the user representation is still valid.
     * 
     * @return <code>true</code> if valid, else <code>false</code>.
     */
    public boolean isValid() {
        return timeUntilValid.after(new Date());
    }

    public Date getTimeUntilValid() {
        return timeUntilValid;
    }
    
    /**
     * Returns the validity of the user in days.
     * @return the user's validity in days.
     */
    public int getValidityInDays(){
        return this.validityInDays;
    }

    /**
     * Checks if the given object represents the same user as this one.
     * @param other the object to check.
     * @return <code>true</code> if it represents the same, else <code>false</code>
     */
    public boolean same(User other) {
        return getUserId().equals(other.getUserId()) && getDomain().equals(other.getDomain());
    }

    @Override
    public String toString() {
        return getUserId() + "@" + getDomain();
    }

    /**
     * Returns the identifier of the represented user.
     * @return the user id.
     */
    public abstract String getUserId();

    /**
     * Returns the domain the represented user belongs to.
     * @return the user's domain.
     */
    public abstract String getDomain();
    
    /**
     * 
     * Returns the type the represented user has.
     * @return the user's type.
     */
    public abstract Type getType();
    
    @Override
    public int hashCode() {
        return getUserId().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof User) {
            User user = (User) o;
            return user.getUserId().equals(getUserId());
        }
        return false;
    }
}

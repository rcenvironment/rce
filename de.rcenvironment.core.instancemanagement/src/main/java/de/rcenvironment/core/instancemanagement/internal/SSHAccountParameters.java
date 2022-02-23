/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

/**
 * provides a parameter object using the builder pattern to create a more readable passing of parameters used to create an uplink 
 * connection.
 * 
 * @author Marlon Schroeter
 */
public class SSHAccountParameters {

    private String userName;

    private String password;
    
    private String userRole;
   
    private boolean isEnabled;

    /**
     * builder to build UplinkConnectionParameters objects. 
     */
    public static class SSHAccountBuilder {

        private SSHAccountParameters parameters = new SSHAccountParameters();

        /**
         * @param userNameParam userName parameter
         * @return builder object to create a fluent interface
         */
        public SSHAccountBuilder userName(String userNameParam) {
            parameters.setUserName(userNameParam);
            return this;
        }

        /**
         * @param passwordParam password parameter
         * @return builder object to create a fluent interface
         */
        public SSHAccountBuilder password(String passwordParam) {
            parameters.setPassword(passwordParam);
            return this;
        } 
        
        /**
         * @param userRoleParam userRole parameter
         * @return builder object to create a fluent interface
         */
        public SSHAccountBuilder userRole(String userRoleParam) {
            parameters.setUserRole(userRoleParam);
            return this;
        }

        /**
         * @param isEnabledParam password parameter
         * @return builder object to create a fluent interface
         */
        public SSHAccountBuilder isEnabled(boolean isEnabledParam) {
            parameters.setEnabled(isEnabledParam);
            return this;
        }

        /**
         * 
         * @return UplinkConnectionParameters object containing all set parameters 
         */
        public SSHAccountParameters build() {
            return this.parameters;
        }
    }
    
    /**
     * 
     * @return UplinkConnectionBuilder object to build UplinkConnectionParameters
     */
    public static SSHAccountBuilder builder() {
        return new SSHAccountBuilder();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean setIsEnabled) {
        this.isEnabled = setIsEnabled;
    }
}

/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

/**
 * Class providing the configuration of the login bundle. Additionally it defines the default
 * configuration.
 * 
 * @author Doreen Seider
 * @author Bea Hornef
 * @author Tobias Menden
 * @author Alice Zorn
 */
public class LoginConfiguration {

    /** Key used to store GUI preferences. */
    public static final String CERTIFICATE_FILE = "certificateFile";

    /** Key used to store GUI preferences. */
    public static final String KEY_FILE = "keyFile";

    /** Key used to store GUI preferences. */
    public static final String USERNAME_LDAP = "usernameLDAP";
    
    /** Key used to store GUI preferences. */
    public static final String TAB_TITLE = "tabTitle";
    /** Key used to store GUI preferences. */
    public static final String ANONYMOUSLOGIN = "anonymousLogin";

    private static final int DEFAULT_LOGIN_VALIDITY = 420;

  

    private String certificateFile = "usercert.pem";

    private String keyFile = "userkey.pem";

    private String ldapUsername = "";
    
    private boolean autoLogin = false;

    private String autoLoginPassword = "";
    
    /**
     * Currently checked Strings: ldap, cert. To change or add Strings see method
     * de.rcenvironment.rce.login.internal.ServiceHandler.activate(BundleContext)
     */
    private String autoLoginMode = "single";

    private int validityInDays = DEFAULT_LOGIN_VALIDITY;
    
    public boolean getAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public String getAutoLoginPassword() {
        return autoLoginPassword;
    }

    public void setAutoLoginPassword(String password) {
        this.autoLoginPassword = password;
    }

    public String getCertificateFile() {
        return certificateFile;
    }

    public void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getLdapUsername() {
        return ldapUsername;
    }
    
    public void setLdapUsername(String newLDAPUsername){
        this.ldapUsername = newLDAPUsername;
    }
    
    public String getAutLoginMode() {
        return autoLoginMode;
    }
    
    public void setAutoLoginMode(String newMode){
        this.autoLoginMode = newMode;
    }

    public void setValidityInDays(int validity){
        this.validityInDays = validity;
    }

    public int getValidityInDays(){
        return validityInDays;
    }

}

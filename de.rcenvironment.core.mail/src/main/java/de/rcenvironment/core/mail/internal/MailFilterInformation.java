/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

//TODO rename?
/**
 * Helper object to store information needed for filtering valid SMTP usernames.
 * 
 * If the host name matches its regex and the username does not match its regex the specified error message should be displayed.
 *
 * @author Tobias Rodehutskors
 */
public class MailFilterInformation {

    private String hostRegex;

    private String usernameRegex;

    private String errorMessage;

    public MailFilterInformation() {
        // default constructor for Jackson
    }
    
    public MailFilterInformation(String hostRegex, String usernameRegex, String errorMessage) {
        this.hostRegex = hostRegex;
        this.usernameRegex = usernameRegex;
        this.errorMessage = errorMessage;
    }
    
    public String getHostRegex() {
        return hostRegex;
    }
    
    public void setHostRegex(String hostRegex) {
        this.hostRegex = hostRegex;
    }
    
    public String getUsernameRegex() {
        return usernameRegex;
    }
    
    public void setUsernameRegex(String usernameRegex) {
        this.usernameRegex = usernameRegex;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

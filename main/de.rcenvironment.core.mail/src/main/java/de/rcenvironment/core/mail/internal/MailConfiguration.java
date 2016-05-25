/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * Class providing the configuration of the mail bundle. Additionally it defines the default
 * configuration.
 * 
 * @author Tobias Menden
 */
public class MailConfiguration {

    private static final int DEFAULT_SSL_PORT = 465;

    private String smtpServer = "smtp.dlr.de";

    private Map<String, String> mailAddressMap = new HashMap<String, String>();

    private String userPass;

    private int sslPort = DEFAULT_SSL_PORT;

    private boolean useSSL = false;

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public void setMaillingLists(Map<String, String> maillingLists) {
        this.mailAddressMap = maillingLists;
    }

    public void setUserPass(String userPass) {
        this.userPass = userPass;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    public int getSslPort() {
        return sslPort;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public Map<String, String> getMaillingLists() {
        return mailAddressMap;
    }

    public String getUserPass() {
        return userPass;
    }

    public boolean getUseSSL() {
        return useSSL;
    }

}

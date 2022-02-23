/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authentication.internal;

import java.util.List;
import java.util.Vector;

/**
 * Provides the configuration of the authentication {@link Bundle} and initialize default
 * configuration values.
 * 
 * @author Doreen Seider
 * @author Tobias Menden
 * @author Alice Zorn
 */
public class  AuthenticationConfiguration   {
    
    private String caFile = "rce_ca.pem";
    
    private String crlFile = "rce_crl.pem";
    
    private List<String> caFilesList = new Vector<String>();
    
    private List<String> crlFilesList = new Vector<String>();
    
    private String server = "intra.dlr.de";
    
    private String baseDn = "DC=intra,DC=dlr,DC=de";
    
    private String domain = "dlr";
    
    
    public AuthenticationConfiguration() {
        if (getCaFiles().isEmpty()){ 
            this.caFilesList.add(caFile);
            setCaFiles(caFilesList); 
        }
        if (getCrlFiles().isEmpty()){ 
            this.crlFilesList.add(crlFile);
            setCrlFiles(crlFilesList);            
        }
    }
    
    public void setCaFiles(List<String> newCaFiles) {
        this.caFilesList = newCaFiles;
    } 
    public void setCrlFiles(List<String> newCrlFiles) {
        this.crlFilesList = newCrlFiles;
    }
    
    public void setLdapServer(String newServer){
        this.server = newServer;
    }
    
    public void setLdapBaseDn(String newBaseDn){
        this.baseDn = newBaseDn;
    }
    
    public void setLdapDomain(String newDomain){
        this.domain = newDomain;
    }
    
    
    public List<String> getCaFiles() {
        return caFilesList;
    }
    
    public List<String> getCrlFiles() {
        return crlFilesList;
    }
    
    public String getLdapServer(){
        return server;
    }
    
    public String getLdapBaseDn(){
        return baseDn;
    }
    
    public String getLdapDomain(){
        return domain;
    }
    
}

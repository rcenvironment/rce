/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * JDBC driver information.
 * 
 * @author Christian Weiss
 */
public class JDBCDriverInformation implements Serializable {

    private static final long serialVersionUID = 212054596109901448L;

    private String url;

    private String file;
    
    private byte[] fileContent;

    private String driver;

    public String getUrl() {
        return url;
    }

    public void setUrl(String prefix) {
        this.url = prefix;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
    
    public byte[] getFileContent() {
        return fileContent;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException("File content serialization not implemented!");
    }
    
    private void readObject(ObjectInputStream in) throws IOException {
        throw new NotSerializableException("File content serialization not implemented!");
    }

}

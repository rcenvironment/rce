/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.osgi.service.log.LogService;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Serializable version of {@link LogEntry}.
 *
 * @author Doreen Seider
 */
public class SerializableLogEntry implements Serializable, Comparable<SerializableLogEntry> {

    /** constant. */
    public static final String RCE_SEPARATOR = "#RCEn";
    
    private static final long serialVersionUID = 1L;

    private final String bundleName;
    private final int level;
    private final String message;
    private final long time;
    private final String exception;
    private NodeIdentifier platformId;

    
    public SerializableLogEntry(String bundleName, int level, String message, long time, String exception) {
        
        this.bundleName = bundleName;
        this.level = level;
        this.message = message;
        this.time = time;
        this.exception = exception;
    }

    public String getBundleName() {
        return bundleName;
    }

    public int getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }
    
    public String getException() {
        return exception;
    }

    public NodeIdentifier getPlatformIdentifer() {
        return platformId;
    }

    public void setPlatformIdentifer(NodeIdentifier newPlatformId) {
        this.platformId = newPlatformId;
    }

    @Override
    public String toString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH:mm:ss,SSS");
        
        String levelAsString = null;
        switch (level) {
        case LogService.LOG_ERROR:
            levelAsString = "ERROR";
            break;
        case LogService.LOG_WARNING:
            levelAsString = "WARNING";
            break;
        case LogService.LOG_INFO:
            levelAsString = "INFO";
            break;
        case LogService.LOG_DEBUG:
            levelAsString = "DEBUG";
            break;
        default:
            break;
        }
        
        return df.format(time) + " " + levelAsString + " - " + message;
    }

    @Override
    public int compareTo(SerializableLogEntry o) {
        
        int compResult = new Date(time).compareTo(new Date(o.getTime()));

        if (compResult == 0) {
            compResult = platformId.toString().compareTo(o.getPlatformIdentifer().toString());

            if (compResult == 0) {
                compResult = bundleName.compareTo(o.getBundleName());
    
                if (compResult == 0) {
                    compResult = new Integer(level).compareTo(new Integer(o.getLevel()));
    
                    if (compResult == 0) {
                        compResult = message.compareTo(o.getMessage());
    
                    }
                }
            }
        }
        
        if (compResult == 0 && exception != null && o.getException() != null) {
            compResult = exception.compareTo(o.getException());
        }

        return compResult;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + bundleName.hashCode();
        result = prime * result +  message.hashCode();
        final int number = 32;
        result = prime * result + (int) (time ^ (time >>> number));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SerializableLogEntry)) {
            return false;
        }
        SerializableLogEntry other = (SerializableLogEntry) obj;
        if (bundleName == null) {
            if (other.bundleName != null) {
                return false;
            }
        } else if (!bundleName.equals(other.bundleName)) {
            return false;
        }
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
            return false;
        }
        if (time != other.time) {
            return false;
        }
        return true;
    }

    
}

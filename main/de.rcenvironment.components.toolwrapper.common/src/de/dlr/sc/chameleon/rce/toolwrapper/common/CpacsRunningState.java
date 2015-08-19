/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.dlr.sc.chameleon.rce.toolwrapper.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * This represents the history object.
 * 
 * @author Markus Kunde
 * @author Arne Bachmann
 */
@Deprecated
public class CpacsRunningState implements Serializable {


    /** serial uid. */
    private static final long serialVersionUID = -176197259790019796L;

    /** CPACS incoming. */
    private String cpacsIn = null;
    
    /** CPACS with singular variables. */
    private String cpacsVariablesIn = null;
    
    /** CPACS outgoing. */
    private String cpacsOut = null;
    
    /** In case of error, store it here. */
    private Throwable error = null;
        
    /**
     * Representation of dynamic channels.
     * Key: Name of Channel
     * Value: String representation of dynamic channel value
     */
    private Map<String, String> dynamicChannelsIn = new HashMap<String, String>();
    
    /**
     * Representation of dynamic channels.
     * Key: Name of Channel
     * Value: String representation of dynamic channel value
     */
    private Map<String, String> dynamicChannelsOut = new HashMap<String, String>();
    
    
    /**
     * Returns incoming CPACS.
     * 
     * @return CPACS, or null
     */
    public String getCpacsIn() {
        return cpacsIn;
    }

    
    /**
     * Sets incoming CPACS.
     * 
     * @param cpacsIncoming CPACS
     */
    public void setCpacsIn(final String cpacsIncoming) {
        cpacsIn = cpacsIncoming;
    }
    
    /**
     * Returns incoming CPACS with mapped variables.
     * 
     * @return CPACS, or null
     */
    public String getCpacsVariableIn() {
        return cpacsVariablesIn;
    }

    
    /**
     * Sets incoming CPACS with mapped variables.
     * 
     * @param cpacsIncoming CPACS
     */
    public void setCpacsVariableIn(final String cpacsIncoming) {
        cpacsVariablesIn = cpacsIncoming;
    }


    
    /**
     * Returns outgoing CPACS.
     * 
     * @return CPACS, or null
     */
    public String getCpacsOut() {
        return cpacsOut;
    }

    
    /**
     * Sets outgoing CPACS.
     * 
     * @param cpacsOutgoing CPACS
     */
    public void setCpacsOut(final String cpacsOutgoing) {
        cpacsOut = cpacsOutgoing;
    }

    
    /**
     * Returns all dynamic input channel values.
     * 
     * @return map of all dynamic input channels. Key is name of channel, Value is value of channel
     */
    public Map<String, String> getDynamicChannelsIn() {
        return Collections.unmodifiableMap(dynamicChannelsIn);
    }

    
    /**
     * Sets an entry regarding a dynamic input channel.
     * 
     * @param entries key name of channel, value of channel as String
     */
    public void setDynamicChannelsInEntry(final Map<String, String> entries) {
        dynamicChannelsIn.putAll(entries);
    }

    
    /**
     * Returns all dynamic output channel values.
     * 
     * @return map of all dynamic output channels. Key is name of channel, Value is value of channel
     */
    public Map<String, String> getDynamicChannelsOut() {
        return Collections.unmodifiableMap(dynamicChannelsOut);
    }

    /**
     * Sets an entry regarding a dynamic output channel.
     * 
     * @param entries key name of channel, value of channel as String
     */
    public void setDynamicChannelsOutEntry(final Map<String, String> entries) {
        dynamicChannelsOut.putAll(entries);
    }
    
    /**
     * If anything goes wrong, at least an exception is stored.
     * @param t The throwable
     */
    public void setError(final Throwable t) {
        error = t;
    }
    
    /**
     * Get the stored error.
     * @return The error
     */
    public Throwable getError() {
        return error;
    }

}

/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

import java.io.File;
import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;


/**
 * Represents one normalized channel value.
 * Normalized means that this class contains plain data regardless of transfer method
 *
 * @author Markus Kunde
 */
public class ChannelValue implements Serializable {

    private static final long serialVersionUID = 4317907370959158263L;

    /**
     * True if value is from input channel.
     */
    private boolean isInputValue = false;
    
    /**
     * Array containing the values of channels.
     */
    private String valuesSerialized = null;
    
    /**
     * Plain text name of channel.
     */
    private String channelName = null;
    
    /**
     * if true all data will be written into Excel even if cell area does not fit.
     */
    private boolean expand = false;
    
    private File excelFile;    
    private String preMacro = null;
    private String runMacro = null;
    private String postMacro = null;
    
    private long iteration;
    
    private ExcelAddress excelAddress;





    /**
     * Constructor.
     * 
     * @param excelFile Excel file
     * @param address Excel address where value(s) are connected to
     * @param channelName name of RCE-Channel
     * @param isInputValue if true channel is an input channel
     * @param expanding if true all data will be written into Excel even if cell area does not fit
     * @param iterationStep iteratin of step
     */
    public ChannelValue(final File excelFile, final ExcelAddress address, final String channelName, final boolean isInputValue, 
        final boolean expanding, final long iterationStep) {
        this.excelFile = excelFile;
        excelAddress = address;
        this.channelName = channelName;
        this.isInputValue = isInputValue;
        iteration = iterationStep;
    }
    
    /**
     * Returns true of value is from input channel.
     * @return Returns the isInputValue.
     */
    public boolean isInputValue() {
        return isInputValue;
    }
    
    /**
     * Returns name of channel.
     * 
     * @return channel name
     */
    public String getChannelName() {
        return channelName;
    }
    
    /**
     * Get iteration step.
     * 
     * @return iteration step
     */
    public long getIteration() {
        return iteration;
    }
    
    /**
     * Gets all values of one concrete channel value. 
     * @return Returns the values or null if no plain values are set.
     */
    public SmallTableTD getValues() {
        TypedDatumSerializer serializer = new ServiceHolder().getSerializer();
        return (SmallTableTD) serializer.deserialize(valuesSerialized);
    }
    
    /**
     * Sets values of a concrete channel value. Should be used if vals are only a few.
     * @param vals The values to set.
     */
    public void setValues(final SmallTableTD vals) {
        TypedDatumSerializer serializer = new ServiceHolder().getSerializer();
        valuesSerialized = serializer.serialize(vals);   
    }
   
    
    /**
     * Returns Excel address.
     * 
     * @return excel address
     */
    public ExcelAddress getExcelAddress() {
        return excelAddress;
    }

    
    /**
     * Returns expanding flat.
     * 
     * @return if true all data will be written into Excel even if cell area does not fit
     */
    public boolean isExpanding() {
        return expand;
    }
    
    /**
     * Get Excel file object.
     * 
     * @return excel file
     */
    public File getFile() {
        return excelFile;
    }
    
    /**
     * Returns name of macro which runs before insertion of input channels.
     * 
     * @return name of macro
     */
    public String getPreMacro() {
        return preMacro;
    }

    
    /**
     * Sets the name of macro which runs before insertion of input channels.
     * 
     * @param preMacro name of macro
     */
    public void setPreMacro(String preMacro) {
        this.preMacro = preMacro;
    }

    /**
     * Returns name of macro which runs after insertion of input channels.
     * 
     * @return name of macro
     */
    public String getRunMacro() {
        return runMacro;
    }

    /**
     * Sets the name of macro which runs after insertion of input channels.
     * 
     * @param runMacro name of macro
     */
    public void setRunMacro(String runMacro) {
        this.runMacro = runMacro;
    }

    /**
     * Returns name of macro which runs after reading of output channels.
     * 
     * @return name of macro
     */
    public String getPostMacro() {
        return postMacro;
    }

    /**
     * Sets the name of macro which runs after reading of output channels.
     * 
     * @param postMacro name of macro
     */
    public void setPostMacro(String postMacro) {
        this.postMacro = postMacro;
    }
}

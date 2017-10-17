/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.database.common;


/**
 * Data class for database statement.
 *
 * @author Oliver Seebach
 */
public class DatabaseStatement {

    private int index;
    
    private String name = "";
    
    private String statement = "";
    
    private boolean willWriteToOutput = false;
    
    private String outputToWriteTo = "";
    
    public DatabaseStatement() {
        
    }

    public int getIndex() {
        return index;
    }

    
    public void setIndex(int index) {
        this.index = index;
    }

    
    public String getName() {
        return name;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    
    public String getStatement() {
        return statement;
    }

    
    public void setStatement(String statement) {
        this.statement = statement;
    }

    
    public boolean isWillWriteToOutput() {
        return willWriteToOutput;
    }

    
    public void setWillWriteToOutput(boolean willWriteToOutput) {
        this.willWriteToOutput = willWriteToOutput;
    }

    
    public String getOutputToWriteTo() {
        return outputToWriteTo;
    }

    
    public void setOutputToWriteTo(String outputToWriteTo) {
        this.outputToWriteTo = outputToWriteTo;
    }

}

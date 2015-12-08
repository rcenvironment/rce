/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.common;


/**
 * Data class containing database component statement pattern and effective statement.
 *
 * @author Oliver Seebach
 */
public class DatabaseStatementHistoryData {

    private String statementPattern;

    private String statementEffective;

    private String statementName;

    private int statementIndex;

    public DatabaseStatementHistoryData() {}

    public DatabaseStatementHistoryData(int statementIndex, String statementName, String statementPattern, String statementEffective) {
        super();
        this.statementPattern = statementPattern;
        this.statementEffective = statementEffective;
        this.statementName = statementName;
        this.statementIndex = statementIndex;
    }

    public String getStatementPattern() {
        return statementPattern;
    }

    public void setStatementPattern(String statementPattern) {
        this.statementPattern = statementPattern;
    }

    public String getStatementEffective() {
        return statementEffective;
    }

    public void setStatementEffective(String statementEffective) {
        this.statementEffective = statementEffective;
    }

    public String getStatementName() {
        return statementName;
    }

    public void setStatementName(String statementName) {
        this.statementName = statementName;
    }

    public int getStatementIndex() {
        return statementIndex;
    }

    public void setStatementIndex(int statementIndex) {
        this.statementIndex = statementIndex;
    }

}

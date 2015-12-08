/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.database.common;

import de.rcenvironment.core.component.api.ComponentConstants;


/**
 * Database component constants.
 *
 * @author Oliver Seebach
 */
public final class DatabaseComponentConstants {

    /** Identifier of the Database component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "database";

    /** Select statement type. */
    public static final String SELECT = "select";

    /** update statement type. */
    public static final String UPDATE = "update";

    /** delete statement type. */
    public static final String DELETE = "delete";

    /** insert statement type. */
    public static final String INSERT = "insert";

    /** List of generally valid statement types. */
    public static final String[] STATEMENT_PREFIX_WHITELIST_GENERAL = { INSERT, DELETE, UPDATE, SELECT };

    /** List of valid statement types with smalltable inputs. */
    public static final String[] STATEMENT_PREFIX_WHITELIST_SMALLTABLE = { INSERT };
    
    /** Property key for database statements. */
    public static final String DB_STATEMENTS_KEY = "databaseStatements"; 

    /** Input placeholder pattern. */
    public static final String INPUT_PLACEHOLDER_PATTERN = "${in:%s}";
    
    /** Identifier component. */
    public static final String STATEMENT_NAME = "StatementName";
    
    /** Identifier component. */
    public static final String DATABASE_NAME = "databaseName";
    
    /** Identifier component. */
    public static final String DATABASE_HOST = "databaseHost";
    
    /** Identifier component. */
    public static final String DATABASE_PORT = "databasePort";
    
    /** Identifier component. */
    public static final String DATABASE_SCHEME = "databaseScheme";
    
    /** Identifier component. */
    public static final String DATABASE_CONNECTOR = "databaseConnector";    
    
    /** Identifier component. */
    public static final String DATABASE_USER = "databaseUser";   
    
    /** Identifier component. */
    public static final String DATABASE_PASSWORD = "databasePassword";
    
    /** Configuration key constant. */
    public static final String CONFIG_KEY_AUTH_USER = "authUser";
    
    /** Configuration key constant. */
    public static final String CONFIG_KEY_AUTH_PHRASE = "authPhrase";
    
    /** Configuration key constant. */
    public static final String NO_OUTPUT_DEFINED_TEXT = "< no outputs defined >";
    
    /** Configuration key constant. */
    public static final String NO_INPUT_DEFINED_TEXT = "< no inputs defined >";
    
    /** Default database statement. */
    public static final String DEFAULT_STATEMENT = "SELECT * FROM table WHERE condition";
    
    
    private DatabaseComponentConstants() { }
    
    /**
     * The Enum DatabaseExecutionType.
     */
    public enum DatabaseExecutionType {

        /** Execute statements sequentially. */
        SEQUENTIALLY("Sequentially"),
        /** Execute statement in parallel. */
        PARALLEL("Parallel");

        /** The title. */
        private final String title;

        /**
         * Instantiates a new type.
         * 
         * @param title the title
         */
        private DatabaseExecutionType(final String title) {
            this.title = title;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return title;
        }

    }
    
    
}

/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.DatabaseStatement;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for Database component.
 *
 * @author Oliver Seebach
 */
public class DatabaseComponentWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    private static final String THE_STATEMENT = "The statement";
    private static final String APOSTROPHE_PERIOD = "'.";
    private static final String APOSTROPHE_BLANK = "' ";
    private static final String SEMICOLON = ";";

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {

        final List<WorkflowNodeValidationMessage> messages = new LinkedList<>();

        // CHECK IF DATABASE (host, port, scheme) IS DEFINED
        checkIfDatabaseDefinitionIsNotEmpty(messages);
        
        // READ STATEMENTS
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        List<DatabaseStatement> models = new ArrayList<>();
        try {
            String modelsString = getProperty(DatabaseComponentConstants.DB_STATEMENTS_KEY);
            if (modelsString != null) {
                models =
                    mapper.readValue(modelsString, mapper.getTypeFactory().constructCollectionType(List.class, DatabaseStatement.class));
            }
        } catch (JsonGenerationException | JsonMappingException e) {
            final WorkflowNodeValidationMessage parsingError =
                new WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                    "Failed to parse database statements.",
                    "Failed to parse database statements.");
            messages.add(parsingError);
        } catch (IOException e) {
            final WorkflowNodeValidationMessage readingFromFilesystemError =
                new WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                    "Failed to read database statements from filesystem.",
                    "Failed to read database statements from filesystem.");
            messages.add(readingFromFilesystemError);
        }

        // CHECK IF ALL STATEMENTS HAVE A NAME -> WARNING
        checkIfAllStatementsAreNamed(messages, models);
        
        // CHECK IF NO STATEMENT IS EMPTY -> ERROR
        checkIfNoStatementIsEmpty(messages, models);
        
        // CHECK VALID BEGINNING / TYPE
        checkIfStatementTypeIsValid(messages, models);

        // CHECK SMALLTABLE JUST IN INSERT
        checkIfSmallTableInputJustInInsert(messages, models);

        // CHECK THAT JUST ONE STATEMENT IS PRESENT
        checkIfJustOneStatementIsPresent(messages, models);

        // CHECK THAT IF WRITE TO OUTPUT IS CHECKED ALSO AN OUTPUT IS SELECTED
        checkIfOutputIsSelectedWhenChecked(messages, models);
        
        // CHECK THAT SELECT STATEMENT ALSO HAS AN OUTPUT DEFINED
        checkIfSelectStatementHasOutput(messages, models);

        return messages;
    }

    private void checkIfAllStatementsAreNamed(final List<WorkflowNodeValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getName().isEmpty()) {
                String warningMessage = "The component has as least one statement with an empty name.";
                final WorkflowNodeValidationMessage statementNameEmpty =
                    new WorkflowNodeValidationMessage(
                        WorkflowNodeValidationMessage.Type.WARNING,
                        DatabaseComponentConstants.DB_STATEMENTS_KEY, warningMessage, warningMessage);
                messages.add(statementNameEmpty);
                break;
            }
        }
    }

    private void checkIfNoStatementIsEmpty(final List<WorkflowNodeValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getStatement().isEmpty()) {
                String warningMessage =
                    THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                        + "' is empty.";
                final WorkflowNodeValidationMessage statementEmpty =
                    new WorkflowNodeValidationMessage(
                        WorkflowNodeValidationMessage.Type.ERROR,
                        DatabaseComponentConstants.DB_STATEMENTS_KEY, warningMessage, warningMessage);
                messages.add(statementEmpty);
            }
        }
    }

    private void checkIfStatementTypeIsValid(final List<WorkflowNodeValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            String statementPrefix = statement.getStatement().toLowerCase().split(" ")[0];
            if (!(Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_GENERAL).contains(statementPrefix))) {
                String warningMessage = THE_STATEMENT + "'" + statement.getName() + "' could not be recognized. "
                    + "Does it not start with 'Select', 'Insert', 'Delete' or 'Update'? "
                    + "See help for further information.";
                final WorkflowNodeValidationMessage notRecognizedStatementType =
                    new WorkflowNodeValidationMessage(
                        WorkflowNodeValidationMessage.Type.ERROR,
                        DatabaseComponentConstants.DB_STATEMENTS_KEY, warningMessage, warningMessage);
                messages.add(notRecognizedStatementType);
            }
        }
    }

    private void checkIfSmallTableInputJustInInsert(final List<WorkflowNodeValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            String statementPrefix = statement.getStatement().toLowerCase().split(" ")[0];
            if (!(Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_SMALLTABLE).contains(statementPrefix))) {
                for (String smalltablePlacerholder : getSmalltableInputPlaceholders()) {
                    if (statement.getStatement().contains(smalltablePlacerholder)) {
                        String warningMessage =
                            THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                                + "' contains an input of type small table but is not an 'insert' statement. The statement is: '"
                                + statement.getStatement() + APOSTROPHE_PERIOD;
                        final WorkflowNodeValidationMessage smalltableInputNotAllowedExceptInsert =
                            new WorkflowNodeValidationMessage(
                                WorkflowNodeValidationMessage.Type.ERROR,
                                DatabaseComponentConstants.DB_STATEMENTS_KEY, warningMessage, warningMessage);
                        messages.add(smalltableInputNotAllowedExceptInsert);
                    }
                }
            }
        }
    }
    
    private List<String> getSmalltableInputPlaceholders() {
        List<String> smalltableInputPlaceholders = new ArrayList<>();
        for (EndpointDescription endpointDescription : getInputs(DataType.SmallTable)) {
            String placeholder = StringUtils.format(DatabaseComponentConstants.INPUT_PLACEHOLDER_PATTERN, endpointDescription.getName());
            smalltableInputPlaceholders.add(placeholder);
        }
        return smalltableInputPlaceholders;
    }

    private void checkIfJustOneStatementIsPresent(final List<WorkflowNodeValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getStatement().contains(SEMICOLON)) {
                String statementAfterFirstSemicolon = statement.getStatement().substring(statement.getStatement().indexOf(SEMICOLON));
                for (String prefix : Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_GENERAL)) {
                    if (statementAfterFirstSemicolon.toLowerCase().contains(prefix)) {
                        String warningMessage =
                            THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                                + "' possibly two statements were entered in the statement textfield. "
                                + "It is '" + statement.getStatement() + APOSTROPHE_PERIOD;
                        final WorkflowNodeValidationMessage possiblyTwoStatements =
                            new WorkflowNodeValidationMessage(
                                WorkflowNodeValidationMessage.Type.WARNING,
                                DatabaseComponentConstants.DB_STATEMENTS_KEY, warningMessage, warningMessage);
                        messages.add(possiblyTwoStatements);
                    }
                }
            }
        }
    }

    private void checkIfSelectStatementHasOutput(final List<WorkflowNodeValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getStatement().trim().toLowerCase().startsWith(DatabaseComponentConstants.SELECT)
                && (!statement.isWillWriteToOutput() || statement.getOutputToWriteTo().isEmpty())) {
                String warningMessage =
                    THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                        + "' is a 'SELECT' statement but has not output defined to write to.";
                final WorkflowNodeValidationMessage selectStatementHasNoOutput =
                    new WorkflowNodeValidationMessage(
                        WorkflowNodeValidationMessage.Type.WARNING,
                        DatabaseComponentConstants.DB_STATEMENTS_KEY, warningMessage, warningMessage);
                messages.add(selectStatementHasNoOutput);
            }
        }
    }

    private void checkIfOutputIsSelectedWhenChecked(final List<WorkflowNodeValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.isWillWriteToOutput() && statement.getOutputToWriteTo().isEmpty()) {
                String warningMessage =
                    THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                        + "' is configured to write to an output but no output is selected yet.";
                final WorkflowNodeValidationMessage outputCheckedButNoDefined =
                    new WorkflowNodeValidationMessage(
                        WorkflowNodeValidationMessage.Type.WARNING,
                        DatabaseComponentConstants.DB_STATEMENTS_KEY, warningMessage, warningMessage);
                messages.add(outputCheckedButNoDefined);
            }
        }
    }

    private void checkIfDatabaseDefinitionIsNotEmpty(final List<WorkflowNodeValidationMessage> messages) {
        if (getProperty(DatabaseComponentConstants.DATABASE_HOST) == null) {
            messages.add(getEmptyHostWarning());
        } else if (getProperty(DatabaseComponentConstants.DATABASE_HOST).isEmpty()) {
            messages.add(getEmptyHostWarning());
        }
        if (getProperty(DatabaseComponentConstants.DATABASE_PORT) == null) {
            messages.add(getEmptyPortWarning());
        } else if (getProperty(DatabaseComponentConstants.DATABASE_PORT).isEmpty()) {
            messages.add(getEmptyPortWarning());
        }
        if (getProperty(DatabaseComponentConstants.DATABASE_SCHEME) == null) {
            messages.add(getEmptySchemeWarning());
        } else if (getProperty(DatabaseComponentConstants.DATABASE_SCHEME).isEmpty()) {
            messages.add(getEmptySchemeWarning());
        }
    }

    private WorkflowNodeValidationMessage getEmptySchemeWarning() {
        String warningMessage =
            "Database Scheme needs to be defined - see Database tab";
        final WorkflowNodeValidationMessage databaseSchemeNull =
            new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DatabaseComponentConstants.DATABASE_SCHEME, warningMessage, warningMessage);
        return databaseSchemeNull;
    }

    private WorkflowNodeValidationMessage getEmptyPortWarning() {
        String warningMessage =
            "Database Port needs to be defined - see Database tab";
        final WorkflowNodeValidationMessage databasePortNull =
            new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DatabaseComponentConstants.DATABASE_PORT, warningMessage, warningMessage);
        return databasePortNull;
    }

    private WorkflowNodeValidationMessage getEmptyHostWarning() {
        String warningMessage =
            "Database Host needs to be defined - see Database tab";
        final WorkflowNodeValidationMessage databaseHostNull =
            new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DatabaseComponentConstants.DATABASE_HOST, warningMessage, warningMessage);
        return databaseHostNull;
    }

}

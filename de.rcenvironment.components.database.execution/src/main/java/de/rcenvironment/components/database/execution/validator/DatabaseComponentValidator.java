/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.execution.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.DatabaseStatement;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for Database component.
 *
 * @author Oliver Seebach
 * @author Jascha Riedel
 */
public class DatabaseComponentValidator extends AbstractComponentValidator {

    private static final String THE_STATEMENT = "The statement";

    private static final String APOSTROPHE_PERIOD = "'.";

    private static final String APOSTROPHE_BLANK = "' ";

    private static final String BLANK_APOSTROPHE = " '";

    private static final String SEMICOLON = ";";

    @Override
    public String getIdentifier() {
        return DatabaseComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {

        List<ComponentValidationMessage> messages = new ArrayList<>();
        // CHECK IF DATABASE (host, port, scheme) IS DEFINED
        checkIfDatabaseDefinitionIsNotEmpty(componentDescription, messages);

        // READ STATEMENTS
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        List<DatabaseStatement> models = new ArrayList<>();
        try {
            String modelsString = getProperty(componentDescription, DatabaseComponentConstants.DB_STATEMENTS_KEY);
            if (modelsString != null) {
                models = mapper.readValue(modelsString,
                    mapper.getTypeFactory().constructCollectionType(List.class, DatabaseStatement.class));
            }
        } catch (JsonGenerationException | JsonMappingException e) {
            final ComponentValidationMessage parsingError = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                "Failed to parse database statements.", "Failed to parse database statements.");
            messages.add(parsingError);
        } catch (IOException e) {
            final ComponentValidationMessage readingFromFilesystemError = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DB_STATEMENTS_KEY,
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
        checkIfSmallTableInputJustInInsert(componentDescription, messages, models);

        // CHECK THAT JUST ONE STATEMENT IS PRESENT
        checkIfJustOneStatementIsPresent(messages, models);

        // CHECK THAT IF WRITE TO OUTPUT IS CHECKED ALSO AN OUTPUT IS SELECTED
        checkIfOutputIsSelectedWhenChecked(messages, models);

        // CHECK THAT SELECT STATEMENT ALSO HAS AN OUTPUT DEFINED
        checkIfSelectStatementHasOutput(messages, models);

        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        return null;
    }

    private void checkIfAllStatementsAreNamed(final List<ComponentValidationMessage> messages,
        List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getName().isEmpty()) {
                String warningMessage = "The component has as least one statement with an empty name.";
                final ComponentValidationMessage statementNameEmpty = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.WARNING, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                    warningMessage, warningMessage);
                messages.add(statementNameEmpty);
                break;
            }
        }
    }

    private void checkIfNoStatementIsEmpty(final List<ComponentValidationMessage> messages,
        List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getStatement().isEmpty()) {
                String warningMessage = THE_STATEMENT + BLANK_APOSTROPHE + statement.getName() + "' is empty.";
                final ComponentValidationMessage statementEmpty = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                    warningMessage, warningMessage);
                messages.add(statementEmpty);
            }
        }
    }

    private void checkIfStatementTypeIsValid(final List<ComponentValidationMessage> messages,
        List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            String statementPrefix = statement.getStatement().toLowerCase().split(" ")[0];
            if (!(Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_GENERAL)
                .contains(statementPrefix))) {
                String warningMessage = THE_STATEMENT + BLANK_APOSTROPHE + statement.getName() + "' could not be recognized. "
                    + "Does it not start with 'Select', 'Insert', 'Delete' or 'Update'? "
                    + "See help for further information.";
                final ComponentValidationMessage notRecognizedStatementType = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                    warningMessage, warningMessage);
                messages.add(notRecognizedStatementType);
            }
        }
    }

    private void checkIfSmallTableInputJustInInsert(ComponentDescription componentDescription,
        final List<ComponentValidationMessage> messages, List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            String statementPrefix = statement.getStatement().toLowerCase().split(" ")[0];
            if (!(Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_SMALLTABLE)
                .contains(statementPrefix))) {
                for (String smalltablePlacerholder : getSmalltableInputPlaceholders(componentDescription)) {
                    if (statement.getStatement().contains(smalltablePlacerholder)) {
                        String warningMessage = THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                            + "' contains an input of type small table but is not an 'insert' statement. The statement is: '"
                            + statement.getStatement() + APOSTROPHE_PERIOD;
                        final ComponentValidationMessage smalltableInputNotAllowedExceptInsert = new ComponentValidationMessage(
                            ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                            warningMessage, warningMessage);
                        messages.add(smalltableInputNotAllowedExceptInsert);
                    }
                }
            }
        }
    }

    private List<String> getSmalltableInputPlaceholders(ComponentDescription componentDescription) {
        List<String> smalltableInputPlaceholders = new ArrayList<>();
        for (EndpointDescription endpointDescription : getInputs(componentDescription, DataType.SmallTable)) {
            String placeholder = StringUtils.format(DatabaseComponentConstants.INPUT_PLACEHOLDER_PATTERN,
                endpointDescription.getName());
            smalltableInputPlaceholders.add(placeholder);
        }
        return smalltableInputPlaceholders;
    }

    private void checkIfJustOneStatementIsPresent(final List<ComponentValidationMessage> messages,
        List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getStatement().contains(SEMICOLON)) {
                String statementAfterFirstSemicolon = statement.getStatement()
                    .substring(statement.getStatement().indexOf(SEMICOLON));
                for (String prefix : Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_GENERAL)) {
                    if (statementAfterFirstSemicolon.toLowerCase().contains(prefix)) {
                        String warningMessage = THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                            + "' possibly two statements were entered in the statement textfield. " + "It is '"
                            + statement.getStatement() + APOSTROPHE_PERIOD;
                        final ComponentValidationMessage possiblyTwoStatements = new ComponentValidationMessage(
                            ComponentValidationMessage.Type.WARNING, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                            warningMessage, warningMessage);
                        messages.add(possiblyTwoStatements);
                    }
                }
            }
        }
    }

    private void checkIfSelectStatementHasOutput(final List<ComponentValidationMessage> messages,
        List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.getStatement().trim().toLowerCase().startsWith(DatabaseComponentConstants.SELECT)
                && (!statement.isWillWriteToOutput() || statement.getOutputToWriteTo().isEmpty())) {
                String warningMessage = THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                    + "' is a 'SELECT' statement but has not output defined to write to.";
                final ComponentValidationMessage selectStatementHasNoOutput = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.WARNING, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                    warningMessage, warningMessage);
                messages.add(selectStatementHasNoOutput);
            }
        }
    }

    private void checkIfOutputIsSelectedWhenChecked(final List<ComponentValidationMessage> messages,
        List<DatabaseStatement> models) {
        for (DatabaseStatement statement : models) {
            if (statement.isWillWriteToOutput() && statement.getOutputToWriteTo().isEmpty()) {
                String warningMessage = THE_STATEMENT + APOSTROPHE_BLANK + statement.getName()
                    + "' is configured to write to an output but no output is selected yet.";
                final ComponentValidationMessage outputCheckedButNoDefined = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.WARNING, DatabaseComponentConstants.DB_STATEMENTS_KEY,
                    warningMessage, warningMessage);
                messages.add(outputCheckedButNoDefined);
            }
        }
    }

    private void checkIfDatabaseDefinitionIsNotEmpty(ComponentDescription componentDescription,
        final List<ComponentValidationMessage> messages) {
        if (getProperty(componentDescription, DatabaseComponentConstants.DATABASE_HOST) == null) {
            messages.add(getEmptyHostWarning());
        } else if (getProperty(componentDescription, DatabaseComponentConstants.DATABASE_HOST).isEmpty()) {
            messages.add(getEmptyHostWarning());
        }
        if (getProperty(componentDescription, DatabaseComponentConstants.DATABASE_PORT) == null) {
            messages.add(getEmptyPortWarning());
        } else if (getProperty(componentDescription, DatabaseComponentConstants.DATABASE_PORT).isEmpty()) {
            messages.add(getEmptyPortWarning());
        }
        if (getProperty(componentDescription, DatabaseComponentConstants.DATABASE_SCHEME) == null) {
            messages.add(getEmptySchemeWarning());
        } else if (getProperty(componentDescription, DatabaseComponentConstants.DATABASE_SCHEME).isEmpty()) {
            messages.add(getEmptySchemeWarning());
        }
    }

    private ComponentValidationMessage getEmptySchemeWarning() {
        String warningMessage = "Database Scheme needs to be defined - see Database tab";
        final ComponentValidationMessage databaseSchemeNull = new ComponentValidationMessage(
            ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DATABASE_SCHEME, warningMessage,
            warningMessage);
        return databaseSchemeNull;
    }

    private ComponentValidationMessage getEmptyPortWarning() {
        String warningMessage = "Database Port needs to be defined - see Database tab";
        final ComponentValidationMessage databasePortNull = new ComponentValidationMessage(
            ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DATABASE_PORT, warningMessage,
            warningMessage);
        return databasePortNull;
    }

    private ComponentValidationMessage getEmptyHostWarning() {
        String warningMessage = "Database Host needs to be defined - see Database tab";
        final ComponentValidationMessage databaseHostNull = new ComponentValidationMessage(
            ComponentValidationMessage.Type.ERROR, DatabaseComponentConstants.DATABASE_HOST, warningMessage,
            warningMessage);
        return databaseHostNull;
    }

}

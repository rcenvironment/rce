/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * {@link CommandPlugin} providing authorization-related console commands.
 *
 * @author Robert Mischke
 */
@Component
public class AuthorizationCommandPlugin implements CommandPlugin {

    private static final String GROUP_ID = "group id";

    private static final String ROOT_COMMAND = "auth";
    
    private static final StringParameter GROUP_ID_PARAMETER = new StringParameter(null, GROUP_ID, "an identifier consisting of 2-32"
            + " letters, numbers, underscores (\"_\") and/or brackets");    

    private static final StringParameter INVITATION_STRING_PARAMETER = new StringParameter(null, "invitation string",
            "imports a group from an invitation string that was previously exported on another node");
    
    private AuthorizationService authorizationService;
    
    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(ROOT_COMMAND, "manage authorization groups",
            "alias for \"auth list\"", this::performList,
            new SubCommandDescription("create", "creates a new authorization group", this::performCreate,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        GROUP_ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("list", "lists the authorization groups that the local node belongs too", this::performList),
            new SubCommandDescription("delete", "deletes a local authorization group", this::performDelete,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        GROUP_ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("export", "exports a group as an invitation string that can be imported by another node, "
                + "allowing that other node to join this group", this::performExport,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        GROUP_ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("import",
                "imports a group from an invitation string that was previously exported on another node", this::performImport,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INVITATION_STRING_PARAMETER
                    }
                )
            )
        );
        
        return new MainCommandDescription[] { commands };
    }
    
    private void performList(CommandContext context) throws CommandException {
        if (context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        final List<AuthorizationAccessGroup> groups = authorizationService.listAccessibleGroups(true);
        for (AuthorizationAccessGroup group : groups) {
            context.println(group.getDisplayName());
        }
    }

    private void performCreate(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        String groupId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        try {
            // TODO decide: check for pre-existing local group or not?
            AuthorizationAccessGroup group = authorizationService.createLocalGroup(groupId);
            context.println(StringUtils.format("Created local group %s (full id: %s)", group.getDisplayName(), group.getFullId()));
        } catch (OperationFailureException e) {
            throw CommandException.executionError(
                "Error creating the new group: " + e.getMessage(), context);
        }
    }

    private void performExport(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        String groupId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        try {
            final AuthorizationAccessGroup group = authorizationService.findLocalGroupById(groupId);
            if (group == null) {
                throw CommandException.executionError(
                    "Failed to export group " + groupId + ": There is no matching local authorized group", context);
            }
            context.println(authorizationService.exportToString(group));
        } catch (OperationFailureException e) {
            throw CommandException.executionError(
                StringUtils.format("Error exporting group %s: %s", groupId, e.getMessage()), context);
        }
    }

    private void performImport(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        String exportString = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        try {
            AuthorizationAccessGroup group = authorizationService.importFromString(exportString);
            context.println("Successfully imported group " + group.getDisplayName());
        } catch (OperationFailureException e) {
            throw CommandException.executionError(
                "Error importing group from invitation string " + exportString + ": " + e.getMessage(), context);
        }
    }

    private void performDelete(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        String groupId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        try {
            final AuthorizationAccessGroup group = authorizationService.findLocalGroupById(groupId);
            if (group == null) {
                throw CommandException.executionError(
                    "Found no local group matching " + groupId + " to delete", context);
            }
            authorizationService.deleteLocalGroupData(group);
            context.println("Deleted local group " + group.getDisplayName());
        } catch (OperationFailureException e) {
            throw CommandException.executionError("Error deleting group " + groupId + ": " + e.getMessage(), context);
        }
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newService) {
        this.authorizationService = newService;
    }

}

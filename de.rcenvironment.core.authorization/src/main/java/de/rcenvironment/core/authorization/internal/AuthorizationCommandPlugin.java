/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * {@link CommandPlugin} providing authorization-related console commands.
 *
 * @author Robert Mischke
 */
@Component
public class AuthorizationCommandPlugin implements CommandPlugin {

    private static final String GROUP_ID_PARAMETER = "<group id>";

    private static final String ROOT_COMMAND = "auth";

    private AuthorizationService authorizationService;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<>();
        contributions.add(new CommandDescription(ROOT_COMMAND, "", false, "short form of \"auth list\""));
        contributions.add(new CommandDescription(ROOT_COMMAND + " create", GROUP_ID_PARAMETER, false, "creates a new authorization group",
            "<group id> - an identifier consisting of 2-32 letters, numbers, underscores (\"_\") and/or brackets"));
        contributions.add(new CommandDescription(ROOT_COMMAND + " list", null, false,
            "lists the authorization groups that the local node belongs too"));
        contributions.add(new CommandDescription(ROOT_COMMAND + " delete", GROUP_ID_PARAMETER, false, "deletes a local authorization group",
            "<group id> - the identifier of the local group to delete"));
        contributions.add(new CommandDescription(ROOT_COMMAND + " export", GROUP_ID_PARAMETER, false,
            "exports a group as an invitation string that can be imported by another node, "
                + "allowing that other node to join this group"));
        contributions.add(new CommandDescription(ROOT_COMMAND + " import", "<invitation string>", false,
            "imports a group from an invitation string that was previously exported on another node"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(ROOT_COMMAND);
        String subCmd = context.consumeNextToken();
        if (subCmd == null || subCmd.equals("list")) {
            performList(context);
        } else if (subCmd.equals("create")) {
            performCreate(context);
        } else if (subCmd.equals("export")) {
            performExport(context);
        } else if (subCmd.equals("import")) {
            performImport(context);
        } else if (subCmd.equals("delete")) {
            performDelete(context);
        } else {
            throw CommandException.unknownCommand(context);
        }
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
        String groupId = fetchSingleParameter(context);
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
        String groupId = fetchSingleParameter(context);
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
        String exportString = fetchSingleParameter(context);
        try {
            AuthorizationAccessGroup group = authorizationService.importFromString(exportString);
            context.println("Successfully imported group " + group.getDisplayName());
        } catch (OperationFailureException e) {
            throw CommandException.executionError(
                "Error importing group from invitation string " + exportString + ": " + e.getMessage(), context);
        }
    }

    private void performDelete(CommandContext context) throws CommandException {
        String groupId = fetchSingleParameter(context);
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

    private String fetchSingleParameter(CommandContext context) throws CommandException {
        final List<String> tokens = context.consumeRemainingTokens();
        if (tokens.size() != 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        String parameter = tokens.get(0);
        return parameter;
    }
}

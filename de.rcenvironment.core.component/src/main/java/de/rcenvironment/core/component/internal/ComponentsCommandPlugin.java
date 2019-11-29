/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.api.DefaultAuthorizationObjects;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.impl.ComponentAuthorizationSelectorImpl;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.incubator.formatter.Alignments;
import de.rcenvironment.core.utils.incubator.formatter.ArrayBasedDataTable;
import de.rcenvironment.core.utils.incubator.formatter.Formatter;

/**
 * A {@link CommandPlugin} providing "components [...]" commands.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */
@Component
public class ComponentsCommandPlugin implements CommandPlugin {

    private static final String ROOT_COMMAND = "components";

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private LocalComponentRegistrationService componentRegistrationService;

    private AuthorizationService authorizationService;

    private DefaultAuthorizationObjects defaultAuthorizationObjects;

    private UserComponentIdMappingService userComponentIdMappingService;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<>();
        contributions.add(new CommandDescription(ROOT_COMMAND, "", false, "short form of \"components list\""));
        contributions.add(new CommandDescription(ROOT_COMMAND + " list", "[--local] [--as-table]", false,
            "show available components; by default, components on the local node as well as "
                + "those published by a reachable remote node are listed. Options:",
            "--local  - only list components provided by the local node",
            // "--remote - only list components from remote nodes",
            "--as-table - format the output as a table that is especially suited for automated parsing"));
        contributions.add(new CommandDescription(ROOT_COMMAND + " list-auth", "", false,
            "Shows a list of all defined authorization settings. "
                + "Note that these settings are independent of whether a matching component exists, "
                + "which means that settings are kept when a component is removed and later added again."));
        contributions.add(new CommandDescription(ROOT_COMMAND + " set-auth", "<component id> <groups>", false,
            "assigns a list of authorization groups to a component id; note that authorization "
                + "settings always apply to all components with using this id, regardless of the component's version",
            "<component id> - A component's id as listed by the \"components list\" command, "
                + "e.g. \"rce/Parametric Study\", \"common/MyIntegratedTool\", or \"cpacs/MyCpacsTool\". "
                + "This id must be enclosed in double quotes if it contains spaces.",
            "<groups> - A comma-separated list of user-defined authorization groups to assign. "
                + "This replaces any previously assigned groups. "
                + "Note that the specified groups must have been created or imported beforehand; "
                + "see the \"auth create\" and \"auth import\" commands for details. "
                + "Instead of a list of groups, the special value \"public\" can be used to grant access to "
                + "any user within the visible network, while \"local\" revokes any previously granted access by remote users."));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(ROOT_COMMAND);
        String firstParameter = context.peekNextToken();
        if (firstParameter == null || firstParameter.startsWith("-")) {
            // "components" shorthand for "components list"; delegate to this without consuming the parameter
            performComponentsList(context);
            return;
        }

        String subCmd = context.consumeNextToken();
        if (subCmd.equals("list")) {
            performComponentsList(context);
        } else if (subCmd.equals("set-auth")) {
            performSetAuth(context);
        } else if (subCmd.equals("list-auth")) {
            performListAuth(context);
        } else {
            throw CommandException.unknownCommand(context);
        }
    }

    @Reference
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        this.componentKnowledgeService = newInstance;
    }

    @Reference
    protected void bindComponentRegistrationService(LocalComponentRegistrationService newInstance) {
        this.componentRegistrationService = newInstance;
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
        this.defaultAuthorizationObjects = newInstance.getDefaultAuthorizationObjects();
    }

    @Reference
    protected void bindUserComponentIdMappingService(UserComponentIdMappingService newInstance) {
        this.userComponentIdMappingService = newInstance;
    }

    private void performComponentsList(CommandContext context) throws CommandException {
        // TreeMap for components ordered alphabetically by platform first, then alphabetically by components
        Map<String, TreeMap<String, DistributedComponentEntry>> components = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        final Collection<DistributedComponentEntry> installationSet;

        final List<String> options = context.consumeRemainingTokens();
        boolean localOnly = options.contains("--local");
        boolean remoteOnly = options.contains("--remote");
        if (localOnly && remoteOnly) {
            throw CommandException.syntaxError("Only one of --local and --remote can be selected", context);
        }
        boolean includeAuthInformation = true; // always on for now; option removed
        boolean asTable = options.contains("--as-table"); // for easier parsing by scripted calls

        if (localOnly) {
            installationSet = compKnowledge.getAllLocalInstallations();
        } else if (remoteOnly) {
            installationSet = compKnowledge.getAllInstallations(); // TODO add actual option once available
        } else {
            installationSet = compKnowledge.getAllInstallations();
        }

        for (DistributedComponentEntry entry : installationSet) {
            ComponentInstallation ci = entry.getComponentInstallation();
            if (components.get(ci.getNodeId()) == null) {
                components.put(ci.getNodeId(), new TreeMap<String, DistributedComponentEntry>(String.CASE_INSENSITIVE_ORDER));
            }
            ComponentInterface compInterface = ci.getComponentInterface();
            String component = compInterface.getDisplayName() + compInterface.getVersion(); // current sorting key
            components.get(ci.getNodeId()).put(component, entry);
        }

        for (String nodeId : components.keySet()) {
            final LogicalNodeId nodeIdObject = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(nodeId);
            if (!asTable) {
                context.println(StringUtils.format("Components available on %s:", nodeIdObject));
            }
            for (DistributedComponentEntry entry : components.get(nodeId).values()) {
                ComponentInstallation ci = entry.getComponentInstallation();
                ComponentInterface compInterface = ci.getComponentInterface();
                String versionPart = compInterface.getVersion();

                // TODO this code needs refactoring; quite a mess to read, and also many redundant calls inside the loop
                final String basePattern;
                if (!asTable) {
                    // default "list" form
                    basePattern = "  %2$s";
                } else {
                    basePattern = "%2$s|%1$s|%3$s";
                }
                if ("".equals(versionPart)) {
                    versionPart = "--";
                }
                final String nodePrefixPattern;
                if (asTable) {
                    nodePrefixPattern = "%5$s|%6$s|";
                } else {
                    nodePrefixPattern = "";
                }

                final String authPatternPart;
                final String authData;

                if (includeAuthInformation) {
                    if (!asTable) {
                        authPatternPart = " <%4$s>";
                    } else {
                        authPatternPart = "|%4$s";
                    }
                    switch (entry.getType()) {
                    case LOCAL:
                        authData = "local";
                        break;
                    case FORCED_LOCAL:
                        authData = "local-only";
                        break;
                    case SHARED:
                        authData = "shared:" + entry.getDeclaredPermissionSet().getSignature();
                        break;
                    case REMOTE:
                        authData = "remote:" + entry.getDeclaredPermissionSet().getSignature();
                        break;
                    default:
                        throw new IllegalArgumentException();
                    }
                } else {
                    authPatternPart = "";
                    authData = "";
                }

                String externalComponentId;
                try {
                    externalComponentId = userComponentIdMappingService.fromInternalToExternalId(compInterface.getIdentifier());
                } catch (OperationFailureException e) {
                    // TODO Auto-generated catch block
                    LogFactory.getLog(getClass()).warn("Failed to determine/generate external id for component "
                        + compInterface.getIdentifier() + "; falling back to display name");
                    externalComponentId = compInterface.getDisplayName();
                }
                context
                    .println(StringUtils.format(nodePrefixPattern + basePattern + authPatternPart, compInterface.getIdentifier(),
                        externalComponentId, versionPart, authData,
                        nodeIdObject.getAssociatedDisplayName(), nodeIdObject.getLogicalNodeIdString()));
            }
        }
    }

    private void performSetAuth(CommandContext context) throws CommandException {
        String componentId = context.consumeNextToken();
        String authSetting = context.consumeNextToken();
        if (authSetting == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        if (componentId.isEmpty() || !componentId.contains("/")) {
            throw CommandException.syntaxError("Invalid component id", context);
        }
        final AuthorizationPermissionSet permissionSet = parsePermissionSetString(authSetting, context);
        ComponentAuthorizationSelectorImpl selector = new ComponentAuthorizationSelectorImpl(componentId);
        componentRegistrationService.setComponentPermissions(selector, permissionSet);
        // fetch actual setting back from service to ensure consistency
        context.println(
            StringUtils.format("Set access authorization for component id \"%s\" to \"%s\"", componentId,
                componentRegistrationService.getComponentPermissionSet(selector, true).getSignature()));
    }

    private AuthorizationPermissionSet parsePermissionSetString(String authSetting, CommandContext context) throws CommandException {
        final AuthorizationPermissionSet permissionSet;
        if ("local".equals(authSetting)) {
            permissionSet = defaultAuthorizationObjects.permissionSetLocalOnly();
        } else if ("public".equals(authSetting)) {
            permissionSet = defaultAuthorizationObjects.permissionSetPublicInLocalNetwork();
        } else {
            final String[] groupIds = authSetting.split(",");
            final List<AuthorizationAccessGroup> groupObjects = new ArrayList<>();
            for (String rawGroupId : groupIds) {
                String groupId = rawGroupId.trim();
                try {
                    final AuthorizationAccessGroup group = authorizationService.findLocalGroupById(groupId);
                    if (group == null) {
                        throw CommandException
                            .executionError("There is no local group matching the id " + groupId, context);
                    }
                    groupObjects.add(group);
                } catch (OperationFailureException e) {
                    throw CommandException
                        .executionError("Error assigning local group " + groupId + ": " + e.getMessage(), context);
                }
            }
            permissionSet = authorizationService.buildPermissionSet(groupObjects);
        }
        return permissionSet;
    }

    private void performListAuth(CommandContext context) {
        // note: expects the list to be sorted by selector id
        List<NamedComponentAuthorizationSelector> externalSelectors =
            componentRegistrationService.listAuthorizationSelectorsForRemotableComponentsIncludingOrphans()
                .stream()
                .filter(selector -> !componentRegistrationService.getComponentPermissionSet(selector, true).isLocalOnly())
                .collect(Collectors.toList());

        if (externalSelectors.isEmpty()) {
            context.println("There are no external access permission(s)");
            return;
        }

        context.println(StringUtils.format("Found %d external access permission(s)", externalSelectors.size()));

        // TODO migrate to table formatter once it has been prepared
        ArrayBasedDataTable outputTable = new ArrayBasedDataTable();
        outputTable.setAlignment(Alignments.LEFT, Alignments.LEFT, Alignments.LEFT);

        final List<NamedComponentAuthorizationSelector> authorizationSelectorsWithoutOrphans =
            componentRegistrationService.listAuthorizationSelectorsForRemotableComponents();
        final boolean allSelectorsAvailable = externalSelectors
            .stream()
            .allMatch(authorizationSelectorsWithoutOrphans::contains);
        for (NamedComponentAuthorizationSelector selector : externalSelectors) {
            final String externalId = selector.getId();
            List<String> tableRow = new ArrayList<>(
                Arrays.asList(externalId, componentRegistrationService.getComponentPermissionSet(selector, true).getSignature()));
            if (!allSelectorsAvailable) {
                final boolean selectorIsAvailable = authorizationSelectorsWithoutOrphans.contains(selector);
                if (selectorIsAvailable) {
                    tableRow.add("");
                } else {
                    tableRow.add("not available");
                }
            }
            outputTable.addRow(tableRow.toArray(new String[0]));
        }
        // ok, the formatter API needs some tweaking...
        for (StringBuilder sb : new Formatter().renderTable(outputTable)) {
            context.println(sb.toString());
        }
    }

}

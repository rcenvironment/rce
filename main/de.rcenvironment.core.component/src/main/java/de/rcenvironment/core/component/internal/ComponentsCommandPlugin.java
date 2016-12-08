/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link CommandPlugin} providing "components [...]" commands.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */
public class ComponentsCommandPlugin implements CommandPlugin {

    private static final String CMD_COMPONENTS = "components";

    private DistributedComponentKnowledgeService componentKnowledgeService;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_COMPONENTS, "", false, "short form of \"components list\""));
        contributions.add(new CommandDescription("components list", "", false, "show components published by reachable RCE nodes"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(CMD_COMPONENTS);
        String subCmd = context.consumeNextToken();
        if (subCmd == null || subCmd.equals("list")) {
            // "components" = "components list"
            performComponentsList(context);
        } else {
            throw CommandException.unknownCommand(context);
        }
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        this.componentKnowledgeService = newInstance;
    }

    private void performComponentsList(CommandContext context) {
        // TreeMap for components ordered alphabetically by platform first, then alphabetically
        // by components
        Map<String, TreeSet<String>> components = new TreeMap<String, TreeSet<String>>(String.CASE_INSENSITIVE_ORDER);

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();
        for (ComponentInstallation ci : compKnowledge.getAllInstallations()) {
            if (components.get(ci.getNodeId()) == null) {
                components.put(ci.getNodeId(), new TreeSet<String>(String.CASE_INSENSITIVE_ORDER));
            }
            ComponentInterface compInterface = ci.getComponentRevision().getComponentInterface();
            String component = compInterface.getDisplayName();
            if (!"".equals(compInterface.getVersion())) {
                component += " (" + compInterface.getVersion() + ")";
            }
            components.get(ci.getNodeId()).add(component);
        }

        for (String nodeId : components.keySet()) {
            context.println(StringUtils.format("Components available on %s:",
                NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(nodeId)));
            for (String component : components.get(nodeId)) {
                context.println("  " + component);
            }
        }
    }

}

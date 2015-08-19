/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;

/**
 * A {@link CommandPlugin} that provides instance management ("im") commands.
 * 
 * @author Robert Mischke
 */
public class InstanceManagementCommandPlugin implements CommandPlugin {

    private static final String ROOT_CMD = "im";

    private InstanceManagementService instanceManagementService;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(ROOT_CMD
            + " install [--if-missing|--force-download|--force-reinstall] <url version id/part> <installation id>", "", false, ""));
        contributions.add(new CommandDescription(ROOT_CMD
            + " configure", "[--clean] <instance id> <template id> [<key=value>*]", false, ""));
        contributions.add(new CommandDescription(ROOT_CMD + " start", "<instance id> <installation id>", false, ""));
        contributions.add(new CommandDescription(ROOT_CMD + " stop", "<instance id>", false, ""));
        contributions.add(new CommandDescription(ROOT_CMD + " dispose", "<instance id>", false, ""));
        contributions.add(new CommandDescription(ROOT_CMD + " list [--instances|--installations|--templates]", "", false, ""));
        contributions.add(new CommandDescription(ROOT_CMD + " info", "", false, ""));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(ROOT_CMD);
        String subCommand = context.consumeNextToken();
        if ("install".equals(subCommand)) {
            performInstall(context);
        } else if ("configure".equals(subCommand)) {
            performConfigure(context);
        } else if ("start".equals(subCommand)) {
            performStart(context);
        } else if ("stop".equals(subCommand)) {
            performStop(context);
        } else if ("list".equals(subCommand)) {
            performList(context);
        } else if ("dispose".equals(subCommand)) {
            performDispose(context);
        } else if ("info".equals(subCommand)) {
            performInformation(context);
        } else {
            throw CommandException.syntaxError("Unknown sub-command", context);
        }
        context.println("Done.");
    }

    protected void bindInstanceManagementService(InstanceManagementService newInstance) {
        this.instanceManagementService = newInstance;
    }

    private void performInstall(CommandContext context) throws CommandException {
        String urlQualifier = context.consumeNextToken();
        String installationId = context.consumeNextToken();
        if (urlQualifier == null || installationId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, context.getOutputReceiver());
        } catch (IOException e) {
            throw CommandException.executionError("Error during installation setup process: " + e.getMessage(), context);
        }
    }

    private void performConfigure(CommandContext context) throws CommandException {
        String instanceId = context.consumeNextToken();
        String templateId = context.consumeNextToken();
        if (instanceId == null || templateId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.configureInstanceFromTemplate(templateId, instanceId, null, false);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performStart(CommandContext context) throws CommandException {
        String instanceId = context.consumeNextToken();
        String installationId = context.consumeNextToken();
        if (instanceId == null || installationId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.startinstance(installationId, instanceId, context.getOutputReceiver());
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performStop(CommandContext context) throws CommandException {
        String instanceId = context.consumeNextToken();
        if (instanceId == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.stopInstance(instanceId, context.getOutputReceiver());
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performList(CommandContext context) throws CommandException {
        String scope = context.consumeNextToken();
        if (scope == null) {
            // list ALL
            scope = "all";
        }
        if ("instances".equals(scope) || "installations".equals(scope) || "templates".equals(scope) || "all".equals(scope)) {
            try {
                instanceManagementService.listInstanceManagementInformation(scope, context.getOutputReceiver());
            } catch (IOException e) {
                throw CommandException.executionError(e.toString(), context);
            }
        } else {
            throw CommandException.syntaxError("Unknown parameter", context);
        }
    }
    
    private void performDispose(CommandContext context) throws CommandException {
        String instanceId = context.consumeNextToken();
        if (instanceId == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.disposeInstance(instanceId, context.getOutputReceiver());
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
        
    }

    private void performInformation(CommandContext context) throws CommandException {
        if (context.hasRemainingTokens()){
            throw CommandException.wrongNumberOfParameters(context);
        }
        instanceManagementService.showInstanceManagementInformation(context.getOutputReceiver());    
    }
}

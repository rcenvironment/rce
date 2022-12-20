/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.api.DefaultAuthorizationObjects;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandParser;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Tests the command plugin responsible for providing the commands starting with `components`.
 * 
 * @author Alexander Weinert
 */
public class ComponentsCommandPluginTest {

    /**
     * The tokens given to the command plugin when "components list-auth" is invoked.
     */
    private static final List<String> COMMAND_TOKENS_LIST_AUTH = Arrays.asList("components", "list-auth");
    
    /**
     * The string output by the command plugin when all tools are only available locally.
     */
    private static final String EXPECTED_MESSAGE_NO_EXTERNAL_PERMISSIONS = "There are no external access permission(s)";

    private AuthorizationService createAuthorizationServiceMock() {
        final AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
        EasyMock.expect(authorizationService.getDefaultAuthorizationObjects())
            .andStubReturn(EasyMock.createMock(DefaultAuthorizationObjects.class));
        EasyMock.replay(authorizationService);
        return authorizationService;
    }

    private NamedComponentAuthorizationSelector createLocalToolMock(final LocalComponentRegistrationService componentRegistrationService,
        final String externalToolId, final String displayName) {
        final NamedComponentAuthorizationSelector selector = createAuthorizationSelector(externalToolId, displayName);
        final AuthorizationPermissionSet localPermissionSet = createLocalPermissionSet();
        EasyMock.expect(componentRegistrationService.getComponentPermissionSet(selector, true)).andStubReturn(localPermissionSet);
        return selector;
    }

    private AuthorizationPermissionSet createLocalPermissionSet() {
        final AuthorizationPermissionSet localPermissionSet = EasyMock.createMock(AuthorizationPermissionSet.class);
        EasyMock.expect(localPermissionSet.isLocalOnly()).andStubReturn(true);
        EasyMock.expect(localPermissionSet.isPublic()).andStubReturn(false);
        EasyMock.expect(localPermissionSet.getSignature()).andStubReturn("Local");
        EasyMock.replay(localPermissionSet);
        return localPermissionSet;
    }
    
    private NamedComponentAuthorizationSelector createPublicToolMock(LocalComponentRegistrationService componentRegistrationService,
        String externalToolId, String displayName) {
        final NamedComponentAuthorizationSelector selector = createAuthorizationSelector(externalToolId, displayName);
        final AuthorizationPermissionSet publicPermissionSet = createPublicPermissionSet();
        EasyMock.expect(componentRegistrationService.getComponentPermissionSet(selector, true)).andStubReturn(publicPermissionSet);
        return selector;
    }
    
    private NamedComponentAuthorizationSelector createGroupToolMock(LocalComponentRegistrationService componentRegistrationService,
        String externalToolId, String displayName, String... groupSignatures) {
        final NamedComponentAuthorizationSelector selector = createAuthorizationSelector(externalToolId, displayName);
        final AuthorizationPermissionSet permissionSet = createPermissionSet(groupSignatures);
        EasyMock.expect(componentRegistrationService.getComponentPermissionSet(selector, true)).andStubReturn(permissionSet);
        return selector;
    }

    private AuthorizationPermissionSet createPermissionSet(String... groupSignatures) {
        final AuthorizationPermissionSet localPermissionSet = EasyMock.createMock(AuthorizationPermissionSet.class);
        EasyMock.expect(localPermissionSet.isLocalOnly()).andStubReturn(false);
        EasyMock.expect(localPermissionSet.isPublic()).andStubReturn(false);
        final String groupSignature = StringUtils.format("<%s>", String.join(";", groupSignatures));
        EasyMock.expect(localPermissionSet.getSignature()).andStubReturn(groupSignature);
        EasyMock.replay(localPermissionSet);
        return localPermissionSet;
    }

    private AuthorizationPermissionSet createPublicPermissionSet() {
        final AuthorizationPermissionSet localPermissionSet = EasyMock.createMock(AuthorizationPermissionSet.class);
        EasyMock.expect(localPermissionSet.isLocalOnly()).andStubReturn(false);
        EasyMock.expect(localPermissionSet.isPublic()).andStubReturn(true);
        EasyMock.expect(localPermissionSet.getSignature()).andStubReturn("Public");
        EasyMock.replay(localPermissionSet);
        return localPermissionSet;
    }

    private NamedComponentAuthorizationSelector createAuthorizationSelector(final String externalToolId, final String displayName) {
        final NamedComponentAuthorizationSelector selector1 = EasyMock.createMock(NamedComponentAuthorizationSelector.class);
        EasyMock.expect(selector1.getId()).andStubReturn(externalToolId);
        EasyMock.expect(selector1.getDisplayName()).andStubReturn(displayName);
        EasyMock.replay(selector1);
        return selector1;
    }
    
    /**
     * @param expectedOutput The expected parameters of the calls to {@link TextOutputReceiver#addOutput(String)}.
     * @return A mock of a TextOutputReceiver that expects only calls to #addOutput with the given Strings as parameters.
     */
    private TextOutputReceiver createOutputReceiverMock(String... expectedOutput) {
        final TextOutputReceiver outputReceiver = EasyMock.createMock(TextOutputReceiver.class);
        for (final String outputLine : expectedOutput) {
            outputReceiver.addOutput(outputLine);
        }
        outputReceiver.onFinished();
        EasyMock.expectLastCall();
        EasyMock.replay(outputReceiver);
        return outputReceiver;
    }

    /**
     * Tests that `components list-auth` gives the correct behavior if no tool is integrated.
     * 
     * @throws CommandException Thrown if {@link ComponentsCommandPlugin#execute(CommandContext)} throws an exception. Not expected.
     */
    @Test
    public void testComponentsListAuthNoComponents() throws CommandException {
        final CommandParser parser = new CommandParser();
        final ComponentsCommandPlugin plugin = new ComponentsCommandPlugin();
        parser.registerCommands(plugin.getCommands());
        
        final DistributedComponentKnowledgeService knowledgeService = EasyMock.createMock(DistributedComponentKnowledgeService.class);
        EasyMock.replay(knowledgeService);
        plugin.bindDistributedComponentKnowledgeService(knowledgeService);

        final LocalComponentRegistrationService componentRegistrationService = EasyMock.createMock(LocalComponentRegistrationService.class);
        EasyMock.expect(componentRegistrationService.listAuthorizationSelectorsForRemotableComponentsIncludingOrphans())
            .andStubReturn(new ArrayList<NamedComponentAuthorizationSelector>());
        EasyMock.replay(componentRegistrationService);
        plugin.bindComponentRegistrationService(componentRegistrationService);

        final AuthorizationService authorizationService = createAuthorizationServiceMock();
        plugin.bindAuthorizationService(authorizationService);

        final UserComponentIdMappingService idMappingService = EasyMock.createMock(UserComponentIdMappingService.class);
        EasyMock.replay(idMappingService);
        plugin.bindUserComponentIdMappingService(idMappingService);
        
        final List<String> originalTokens = COMMAND_TOKENS_LIST_AUTH;
        final TextOutputReceiver outputReceiver = createOutputReceiverMock(EXPECTED_MESSAGE_NO_EXTERNAL_PERMISSIONS);

        final CommandContext context = new CommandContext(originalTokens, outputReceiver, new Object());
        
        parser.parseCommand(context).execute();

        EasyMock.verify(outputReceiver);
    }

    /**
     * Tests that `components list-auth` gives the correct behavior if a tool is integrated, but it is not published.
     * 
     * @throws CommandException Thrown if {@link ComponentsCommandPlugin#execute(CommandContext)} throws an exception. Not expected.
     */
    @Test
    public void testComponentsListAuthOneComponentOneLocalOneAvailable() throws CommandException {
        final CommandParser parser = new CommandParser();
        final ComponentsCommandPlugin plugin = new ComponentsCommandPlugin();
        parser.registerCommands(plugin.getCommands());
        
        final DistributedComponentKnowledgeService knowledgeService = EasyMock.createMock(DistributedComponentKnowledgeService.class);
        EasyMock.replay(knowledgeService);
        plugin.bindDistributedComponentKnowledgeService(knowledgeService);

        final LocalComponentRegistrationService componentRegistrationService = EasyMock.createMock(LocalComponentRegistrationService.class);
        final NamedComponentAuthorizationSelector commonSelector =
            createLocalToolMock(componentRegistrationService, "common/commonTool", "Common Tool");
        EasyMock.expect(componentRegistrationService.listAuthorizationSelectorsForRemotableComponentsIncludingOrphans())
            .andStubReturn(Arrays.asList(commonSelector));
        EasyMock.replay(componentRegistrationService);
        plugin.bindComponentRegistrationService(componentRegistrationService);

        final AuthorizationService authorizationService = createAuthorizationServiceMock();
        plugin.bindAuthorizationService(authorizationService);

        final UserComponentIdMappingService idMappingService = EasyMock.createMock(UserComponentIdMappingService.class);
        EasyMock.replay(idMappingService);
        plugin.bindUserComponentIdMappingService(idMappingService);
        
        final List<String> originalTokens = COMMAND_TOKENS_LIST_AUTH;
        final TextOutputReceiver outputReceiver = createOutputReceiverMock(EXPECTED_MESSAGE_NO_EXTERNAL_PERMISSIONS);

        final CommandContext context = new CommandContext(originalTokens, outputReceiver, new Object());
        
        parser.parseCommand(context).execute();

        EasyMock.verify(outputReceiver);
    }

    /**
     * Tests that `components list-auth` gives the correct behavior if three tools are integrated, one is public, one is published in a
     * group, one is local, and all three are available.
     * 
     * @throws CommandException Thrown if {@link ComponentsCommandPlugin#execute(CommandContext)} throws an exception. Not expected.
     */
    @Test
    public void testComponentsListAuthThreeComponentsOneLocalOnePublicThreeAvailable() throws CommandException {
        final CommandParser parser = new CommandParser();
        final ComponentsCommandPlugin plugin = new ComponentsCommandPlugin();
        parser.registerCommands(plugin.getCommands());
        
        final DistributedComponentKnowledgeService knowledgeService = EasyMock.createMock(DistributedComponentKnowledgeService.class);
        EasyMock.replay(knowledgeService);
        plugin.bindDistributedComponentKnowledgeService(knowledgeService);

        final LocalComponentRegistrationService componentRegistrationService = EasyMock.createMock(LocalComponentRegistrationService.class);
        final NamedComponentAuthorizationSelector localSelector =
            createLocalToolMock(componentRegistrationService, "common/localTool", "LocalTool Tool");
        final NamedComponentAuthorizationSelector publicSelector =
            createPublicToolMock(componentRegistrationService, "common/publicTool", "Public Tool");
        final NamedComponentAuthorizationSelector groupSelector =
            createGroupToolMock(componentRegistrationService, "common/groupTool", "Group Tool", "GroupA:ABCDEF", "GroupB:123456");

        EasyMock.expect(componentRegistrationService.listAuthorizationSelectorsForRemotableComponentsIncludingOrphans())
            .andStubReturn(Arrays.asList(localSelector, publicSelector, groupSelector));
        EasyMock.expect(componentRegistrationService.listAuthorizationSelectorsForRemotableComponents())
            .andStubReturn(Arrays.asList(localSelector, publicSelector, groupSelector));
        EasyMock.replay(componentRegistrationService);
        plugin.bindComponentRegistrationService(componentRegistrationService);

        final AuthorizationService authorizationService = createAuthorizationServiceMock();
        plugin.bindAuthorizationService(authorizationService);

        final UserComponentIdMappingService idMappingService = EasyMock.createMock(UserComponentIdMappingService.class);
        EasyMock.replay(idMappingService);
        plugin.bindUserComponentIdMappingService(idMappingService);
        
        final List<String> originalTokens = COMMAND_TOKENS_LIST_AUTH;
        final TextOutputReceiver outputReceiver = createOutputReceiverMock(
            "Found 2 external access permission(s)",
            "|common/publicTool|Public                       |",
            "|common/groupTool |<GroupA:ABCDEF;GroupB:123456>|");

        final CommandContext context = new CommandContext(originalTokens, outputReceiver, new Object());
        
        parser.parseCommand(context).execute();

        EasyMock.verify(outputReceiver);
    }

    /**
     * Tests that `components list-auth` gives the correct behavior if three tools are integrated, one is public, one is published in a
     * group, one is local, and all only the public one is available.
     * 
     * @throws CommandException Thrown if {@link ComponentsCommandPlugin#execute(CommandContext)} throws an exception. Not expected.
     * @throws OperationFailureException Not thrown, since the method potentially throwing this is only called on a mock.
     */
    @Test
    public void testComponentsListAuthThreeComponentsOneLocalOnePublicOneAvailable() throws CommandException, OperationFailureException {
        final CommandParser parser = new CommandParser();
        final ComponentsCommandPlugin plugin = new ComponentsCommandPlugin();
        parser.registerCommands(plugin.getCommands());

        final DistributedComponentKnowledgeService knowledgeService = EasyMock.createMock(DistributedComponentKnowledgeService.class);
        final DistributedComponentKnowledge snapshot = EasyMock.createMock(DistributedComponentKnowledge.class);
        EasyMock.expect(knowledgeService.getCurrentSnapshot()).andStubReturn(snapshot);
        EasyMock.replay(snapshot, knowledgeService);
        plugin.bindDistributedComponentKnowledgeService(knowledgeService);

        final LocalComponentRegistrationService componentRegistrationService = EasyMock.createMock(LocalComponentRegistrationService.class);
        final NamedComponentAuthorizationSelector localSelector =
            createLocalToolMock(componentRegistrationService, "common/localTool", "LocalTool Tool");
        final NamedComponentAuthorizationSelector publicSelector =
            createPublicToolMock(componentRegistrationService, "common/publicTool", "Public Tool");
        final NamedComponentAuthorizationSelector groupSelector =
            createGroupToolMock(componentRegistrationService, "common/groupTool", "Group Tool", "GroupA:ABCDEF", "GroupB:123456");

        EasyMock.expect(componentRegistrationService.listAuthorizationSelectorsForRemotableComponentsIncludingOrphans())
            .andStubReturn(Arrays.asList(localSelector, publicSelector, groupSelector));
        EasyMock.expect(componentRegistrationService.listAuthorizationSelectorsForRemotableComponents())
            .andStubReturn(Arrays.asList(publicSelector));
        EasyMock.replay(componentRegistrationService);
        plugin.bindComponentRegistrationService(componentRegistrationService);

        final AuthorizationService authorizationService = createAuthorizationServiceMock();
        plugin.bindAuthorizationService(authorizationService);

        final UserComponentIdMappingService idMappingService = EasyMock.createMock(UserComponentIdMappingService.class);
        EasyMock.replay(idMappingService);
        plugin.bindUserComponentIdMappingService(idMappingService);
        
        final List<String> originalTokens = COMMAND_TOKENS_LIST_AUTH;
        final TextOutputReceiver outputReceiver = createOutputReceiverMock(
            "Found 2 external access permission(s)",
            "|common/publicTool|Public                       |             |",
            "|common/groupTool |<GroupA:ABCDEF;GroupB:123456>|not available|");

        final CommandContext context = new CommandContext(originalTokens, outputReceiver, new Object());

        parser.parseCommand(context).execute();

        EasyMock.verify(outputReceiver);
    }
}

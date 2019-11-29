/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.internal.handlers;

import java.util.LinkedList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Tests for {@link BuiltInCommandPlugin}.
 *
 * @author Tobias Rodehutskors
 */
public class BuiltInCommandPluginTest {

    /**
     * Expected exception placeholder.
     */
    @Rule
    public ExpectedException commandException = ExpectedException.none();

    /**
     * Tests if the force-crash command throws a reasonable exception if no argument was supplied.
     * 
     * @throws CommandException expected
     */
    @Test
    public void testForceCrashWithoutArgument() throws CommandException {

        BuiltInCommandPlugin plugin = new BuiltInCommandPlugin();
        TextOutputReceiver receiver = EasyMock.createStrictMock(TextOutputReceiver.class);

        List<String> tokens = new LinkedList<String>();
        tokens.add("force-crash");

        CommandContext context = new CommandContext(tokens, receiver, "invoker");

        commandException.expect(CommandException.class);
        commandException.expectMessage("Wrong number of parameters");
        plugin.execute(context);
    }


    /**
     * Tests if the force-crash command throws a reasonable exception if an invalid argument was supplied.
     * 
     * @throws CommandException expected
     */
    @Test
    public void testForceCrashWithArgumentOfWrongType() throws CommandException {

        BuiltInCommandPlugin plugin = new BuiltInCommandPlugin();
        TextOutputReceiver receiver = EasyMock.createStrictMock(TextOutputReceiver.class);

        List<String> tokens = new LinkedList<String>();
        tokens.add("force-crash");
        tokens.add("string");

        CommandContext context = new CommandContext(tokens, receiver, "invoker");

        commandException.expect(CommandException.class);
        commandException.expectMessage("You need to specify the delay in milliseconds");
        plugin.execute(context);
    }
}

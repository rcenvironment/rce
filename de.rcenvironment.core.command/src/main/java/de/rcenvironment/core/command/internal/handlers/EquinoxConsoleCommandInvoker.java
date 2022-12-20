/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.internal.handlers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A utility class that attempts to execute OSGi commands as if they were entered into the Equinox command console.
 * 
 * To achieve this, the GOGO's CommandProcessor service is used, it is invoked, and the generated output is forwarded to the caller. Only
 * commands implemented by the EquinoxCommandProvider are supported.
 * 
 * @author Robert Mischke
 * @author Tobias Brieden (adaptation for the GOGO shell)
 */
public class EquinoxConsoleCommandInvoker {

    private static final String OSGI_COMMAND_FUNCTION = "osgi.command.function";

    private static final String OSGI_COMMAND_SCOPE = "osgi.command.scope";

    private final Log log = LogFactory.getLog(getClass());

    private CommandContext context;

    
    /**
     * Attempts to execute the OSGi console command defined by the remaining tokens in the given {@link CommandContext}. See class
     * description for the general approach.
     * 
     * @param pContext the {@link CommandContext} providing parsed command parameters and output receiver
     * @throws CommandException if an error occurs during command execution
     */
    public void execute(final CommandContext pContext) throws CommandException {
        this.execute(pContext, pContext.getOutputReceiver());
    }
    
    /**
     * Attempts to execute the OSGi console command defined by the remaining tokens in the given {@link CommandContext}. See class
     * description for the general approach.
     * 
     * @param pContext the {@link CommandContext} providing parsed command parameters
     * @param outputReceiver the output receiver to be used
     * @throws CommandException if an error occurs during command execution
     */
    public void execute(final CommandContext pContext, TextOutputReceiver outputReceiver) throws CommandException {
        this.context = pContext;

        ParsedCommandModifiers modifiers = pContext.getParsedModifiers();
        
        ParsedStringParameter osgiCommandParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        
        String osgiCommand = osgiCommandParameter.getResult().split(" ")[0];
        if (osgiCommand == null || osgiCommand.isEmpty()) {
            throw CommandException.syntaxError("Missing OSGi command", context);
        }

        // For now, we are only supporting OSGi command which are implemented by the EquinoxCommandProvider. Therefore we retrieve a
        // reference to this class and check if it implements the requested command...
        boolean matched;
        try {
            matched = equinoxCommandProviderImplementsRequestedCommand(osgiCommand);
        } catch (IllegalStateException e) {
            return;
        }

        // ... if this is not the case, we return ...
        if (!matched) {
            outputReceiver.addOutput("No matching OSGi command found. "
                + "(Note that built-in commands like \"info\" or \"help\" may not be accessible.)");
            return;
        }

        // ... otherwise we execute the command
        // TODO We cannot use DS here, since the BuiltInCommandPlugin calls the constructor of this class explicitly.
        final BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<?> sref = bundleContext.getServiceReference("org.apache.felix.service.command.CommandProcessor");
        CommandProcessor commandProcessorService = (CommandProcessor) bundleContext.getService(sref);

        // create an output stream for the command session
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        CommandSession session = commandProcessorService.createSession(IOUtils.toInputStream(""), ps, ps);

        try {
            // execute the OSGi command with its supplied parameters
            List<String> tokens = Arrays.asList(osgiCommandParameter.getResult().split(" "));
            tokens.remove(0);
            String completeOsgiCommand = osgiCommand + " " + StringUtils.join(tokens, ' ');
            session.execute(completeOsgiCommand);
            // CHECKSTYLE:DISABLE (IllegalCatch) - we need to catch Exception since execute does not throw a more specific one
        } catch (Exception e) {
            // CHECKSTYLE:ENABLE (IllegalCatch)
            outputReceiver.addOutput("An error occured during the execution of the OSGi command.");
        }
        session.close();
        // print the command output
        outputReceiver.addOutput(baos.toString());

    }

    private boolean equinoxCommandProviderImplementsRequestedCommand(String osgiCommand) {

        final BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<?>[] refs;
        try {
            String filter = String.format("(&(%s=*)(%s=*))", OSGI_COMMAND_SCOPE, OSGI_COMMAND_FUNCTION);
            // If we remove the class name of the next call and replace it with null, we get a list of all registered services that
            // implement commands. For now, we only support the command implemented by the EquinoxCommandProvider.
            refs = bundleContext.getAllServiceReferences("org.eclipse.equinox.console.commands.EquinoxCommandProvider", filter);
        } catch (InvalidSyntaxException e) {
            log.error("Error getting service references", e);
            context.println("Internal error");
            throw new IllegalStateException();
        }

        boolean matched = false;
        for (ServiceReference<?> ref : refs) {
            // String scope = ref.getProperty(OSGI_COMMAND_SCOPE).toString();
            Object functions = ref.getProperty(OSGI_COMMAND_FUNCTION);

            if (functions.getClass().isArray()) {
                String[] functionsArray = (String[]) functions;
                matched = Arrays.asList(functionsArray).contains(osgiCommand);
            } else {
                context.println("Error: Functions is not an array!");
                throw new IllegalStateException();
            }

            if (matched) {
                break;
            }
        }

        return matched;
    }
}

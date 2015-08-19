/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.internal.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;

/**
 * A utility class that attempts to execute OSGi commands as if they were entered into the Equinox command console.
 * 
 * To achieve this, all registered OSGi {@link CommandProvider} services are examined for matching command handlers methods (which are
 * named, by OSGi convention, as the command name prefixed with an underscore character). If a matching handler is found, it is invoked, and
 * the generated output is forwarded to the caller.
 * 
 * @author Robert Mischke
 */
public class EquinoxConsoleCommandInvoker {

    private static final String MSG_ERROR_INVOKING_OSGI_COMMAND = "Error invoking OSGi command";

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Attempts to execute the OSGi console command defined by the remaining tokens in the given {@link CommandContext}. See class
     * description for the general approach.
     * 
     * @param context the {@link CommandContext} providing command tokens and receiving output
     * @throws CommandException if an error occurs during command execution
     */
    public void execute(final CommandContext context) throws CommandException {
        final CommandInterpreter commandInterpreter = createCommandInterpreter(context);
        String osgiCommand = context.consumeNextToken();
        if (osgiCommand == null || osgiCommand.isEmpty()) {
            throw CommandException.syntaxError("Missing OSGi command", context);
        }

        final BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference[] refs;
        try {
            refs = bundleContext.getAllServiceReferences(CommandProvider.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            log.error("Error getting service references", e);
            context.println("Internal error");
            return;
        }
        final String commandMethodName = "_" + osgiCommand;
        boolean matched = false;
        for (ServiceReference<?> ref : refs) {
            final CommandProvider service = (CommandProvider) bundleContext.getService(ref);
            final Method[] methods = service.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals(commandMethodName)) {
                    invokeServiceMethod(service, method, commandInterpreter);
                    matched = true;
                }
            }
            bundleContext.ungetService(ref);
        }
        if (!matched) {
            context.println("No matching OSGi command found. "
                + "(Note that built-in commands like \"info\" or \"help\" may not be accessible.)");
        }
    }

    private void invokeServiceMethod(final CommandProvider service, Method method, final CommandInterpreter commandInterpreter) {
        try {
            method.invoke(service, commandInterpreter);
        } catch (RuntimeException e) {
            // TODO improve handling: generate console feedback as well?
            log.error(MSG_ERROR_INVOKING_OSGI_COMMAND, e);
        } catch (IllegalAccessException e) {
            log.error(MSG_ERROR_INVOKING_OSGI_COMMAND, e);
        } catch (InvocationTargetException e) {
            log.error(MSG_ERROR_INVOKING_OSGI_COMMAND, e);
        }
    }

    private static CommandInterpreter createCommandInterpreter(final CommandContext context) {
        return new CommandInterpreter() {

            private StringBuilder lineBuffer;

            @Override
            public void print(Object arg0) {
                if (lineBuffer == null) {
                    lineBuffer = new StringBuilder();
                }
                lineBuffer.append(arg0.toString());
            }

            @Override
            public void println(Object arg0) {
                if (lineBuffer != null) {
                    lineBuffer.append(arg0.toString());
                    context.println(lineBuffer.toString());
                    lineBuffer = null;
                } else {
                    context.println(arg0);
                }
            }

            @Override
            public void println() {
                println("");
            }

            @Override
            public void printStackTrace(Throwable arg0) {
                LogFactory.getLog(getClass()).warn("Unhandled printStackTrace output: " + arg0);
            }

            @Override
            @SuppressWarnings("rawtypes")
            public void printDictionary(Dictionary arg0, String arg1) {
                LogFactory.getLog(getClass()).warn("Unhandled printDictionary output: " + arg1);
            }

            @Override
            public void printBundleResource(Bundle arg0, String arg1) {
                LogFactory.getLog(getClass()).warn("Unhandled printBundleResource output: " + arg1);
            }

            @Override
            public String nextArgument() {
                return context.consumeNextToken();
            }

            @Override
            public Object execute(String arg0) {
                return null;
            }
        };
    }
}

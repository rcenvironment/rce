/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.utils.common.components;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.notification.DistributedNotificationService;

/**
 * ConsoleLogger wraps and help sending String lines to the rce console output logger.
 * 
 * Note: Class is deprecated because RCE for CPACS should use default RCE-technology of executing command-line calls and catching every
 * output regarding std-out/err.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 */
@Deprecated
public class ConsoleLogger extends OutputStream {

    private static final String STRING_ENDING_CONSTANT = "\n";

    /**
     * The ComponentInformation of the rce component that wants to use the logger.
     */
    private final ComponentContext componentContext;

    private final ConsoleRow.Type type;

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private List<OutputStream> additionalOutputStreams = new ArrayList<OutputStream>();

    /**
     * Constructor.
     * 
     * @param ci - The ComponentInformation of the rce component that wants to use the logger.
     * @param t - console row type.
     * @param service - The notification service used to send the console lines.
     */
    public ConsoleLogger(final ComponentContext ctx, final DistributedNotificationService service, final ConsoleRow.Type t) {
        componentContext = ctx;
        type = t;
    }

    /**
     * Register additional output stream.
     * 
     * @param ostream additional stream.
     */
    public void registerAdditionalOutputStream(OutputStream ostream) {
        additionalOutputStreams.add(ostream);
    }

    /**
     * Sends an one-line String to component console logger.
     * 
     * @param message - The message to send.
     */
    private void stdPrint(final String message) {
        componentContext.printConsoleLine(message, type);
    }

    @Override
    public void write(int b) throws IOException {
        baos.write(b);
        if (baos.toString().contains(STRING_ENDING_CONSTANT)) {
            String line = baos.toString();
            line = StringUtils.removeEndIgnoreCase(line, STRING_ENDING_CONSTANT);
            if (StringUtils.isNotBlank(line)) {
                stdPrint(line);
                for (OutputStream ostream : additionalOutputStreams) {
                    Writer fileOutput = new BufferedWriter(new OutputStreamWriter(ostream));
                    fileOutput.write(line);
                    fileOutput.flush();
                }

            }
            baos.reset();
        }
    }

    /**
     * Closes and releases any additional stream.
     * 
     * @throws ComponentException if IOException occurs
     * 
     */
    public void closeAndReleaseStreams() throws ComponentException {
        for (OutputStream ostream : additionalOutputStreams) {
            try {
                ostream.flush();
                ostream.close();
            } catch (IOException e) {
                throw new ComponentException(e);
            }
        }

        additionalOutputStreams.clear();
    }
}

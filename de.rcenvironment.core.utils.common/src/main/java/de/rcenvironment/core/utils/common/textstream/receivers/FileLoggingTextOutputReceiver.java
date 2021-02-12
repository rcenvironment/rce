/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A simple implementation of {@link TextOutputReceiver} that logs all received events to a file.
 * 
 * @author Tobias Rodehutskors
 * 
 */
public class FileLoggingTextOutputReceiver implements TextOutputReceiver {

    private final Log log = LogFactory.getLog(getClass());

    private Path filepath;

    private BufferedWriter writer;

    /**
     * @param file path to a file into which the output should we written
     */
    public FileLoggingTextOutputReceiver(Path file) {
        filepath = file;
    }

    @Override
    public void onStart() {
        try {
            FileOutputStream fos = new FileOutputStream(filepath.toFile());
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            writer = new BufferedWriter(osw);
        } catch (FileNotFoundException e) {
            log.error("File not found.", e);
        }
    }

    @Override
    public void onFinished() {
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Unable to close the output writer.", e);
        }
    }

    @Override
    public void onFatalError(Exception e) {

        try {
            writer.write(e.toString());
        } catch (IOException ioException) {
            log.error("Unable to write exception.", e);
        }

        onFinished();
    }

    @Override
    public void addOutput(String line) {
        try {
            writer.write(line);
        } catch (IOException e) {
            log.error("Unable to write.", e);
        }
    }

}

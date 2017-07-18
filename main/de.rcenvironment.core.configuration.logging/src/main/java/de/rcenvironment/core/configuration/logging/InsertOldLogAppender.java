/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.RollingFileAppender;

/**
 * This appender extends the {@link RollingFileAppender}, but in addition, it copies the content of another log file to the start of the
 * new log file.
 *
 * @author Tobias Brieden
 */
public class InsertOldLogAppender extends RollingFileAppender {

    private String oldFileLocation;

    public void setOldFile(String oldFile) {
        this.oldFileLocation = oldFile;
    }

    @Override
    public void activateOptions() {
        super.activateOptions();

        try {
            List<String> lines = Files.readAllLines(Paths.get(oldFileLocation), StandardCharsets.UTF_8);

            for (String line : lines) {
                this.qw.write(line);
                this.qw.write(System.lineSeparator());
            }

            this.qw.flush();
            Files.delete(Paths.get(oldFileLocation));

        } catch (IOException e) {
            this.qw.write("Unable to copy the startup log to the final log location: " + e.getMessage());
        }
    };
}

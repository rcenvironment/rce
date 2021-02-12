/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.introduction;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents the Javascript file governing the behavior of the welcome screen. The welcome screen contains a checkbox that allows the user
 * to skip the welcome screen upon next startup of RCE. The value of that checkbox is set upon instantiation of the welcome screen via the
 * Javascript file represented by this class. That file contains a line in which the value of that checkbox is set via javascript code. We
 * call that line the Checkbox line.
 * 
 * The main convenience afforded by this class is the capability to rewrite the javascript file and to replace the checkbox line by one
 * initializing the checkbox to a given value.
 *
 * @author Alexander Weinert
 */
final class WelcomeScreenScriptFile {

    private static final String CHECKBOX_LINE_MARKER = "checkbox.checked = ";

    private static final String CHECKBOX_LINE_IF_UNCHECKED = "\tcheckbox.checked = false;";

    private static final String CHECKBOX_LINE_IF_CHECKED = "\tcheckbox.checked = true;";

    private final Log log = LogFactory.getLog(getClass());

    private final String absolutePathToScript;

    private WelcomeScreenScriptFile(final String pathToScript) {
        this.absolutePathToScript = pathToScript;
    }

    public static WelcomeScreenScriptFile createFromAbsolutePath(final String absolutePathToScript) {
        return new WelcomeScreenScriptFile(absolutePathToScript);
    }

    public void persistUserChoice(final boolean checkboxState) {
        final String replacementCheckboxLine = checkboxLineForCheckboxState(checkboxState);

        final Optional<byte[]> fileWithReplacedLine = readFileReplaceCheckboxLineAndHandleErrors(replacementCheckboxLine);
        fileWithReplacedLine.ifPresent(content -> writeFileWithErrorHandling(content));
    }

    private Optional<byte[]> readFileReplaceCheckboxLineAndHandleErrors(final String replacementCheckboxLine) {
        try (Stream<String> scriptLines = getScriptFileLineStream()) {
            final byte[] newFileContents = readFileAndReplaceCheckboxLine(scriptLines, replacementCheckboxLine);
            return Optional.ofNullable(newFileContents);
        } catch (IOException e) {
            logReadingExceptionAsError(e);
            return Optional.empty();
        }
    }

    private Stream<String> getScriptFileLineStream() throws IOException {
        final Path pathToScriptFile = Paths.get(this.absolutePathToScript);
        return Files.lines(pathToScriptFile);
    }

    private byte[] readFileAndReplaceCheckboxLine(final Stream<String> lines, final String replacementCheckboxLine) throws IOException {
        return lines
            .map(line -> replaceLineIfCheckboxLine(line, replacementCheckboxLine))
            .collect(Collectors.joining("\n"))
            .getBytes();
    }

    private String replaceLineIfCheckboxLine(String line, String replacementCheckboxLine) {
        if (lineIsCheckboxLine(line)) {
            return replacementCheckboxLine;
        } else {
            return line;
        }
    }

    private boolean lineIsCheckboxLine(String line) {
        return line.contains(CHECKBOX_LINE_MARKER);
    }

    private void writeFileWithErrorHandling(final byte[] newFileContents) {
        try (FileOutputStream scriptFileOutputStream = buildFileOutputStream()) {
            scriptFileOutputStream.write(newFileContents);
        } catch (IOException e) {
            logWritingExceptionAsError(e);
        }
    }

    private FileOutputStream buildFileOutputStream() throws FileNotFoundException {
        return new FileOutputStream(this.absolutePathToScript);
    }

    private String checkboxLineForCheckboxState(final boolean checkboxState) {
        if (checkboxState) {
            return CHECKBOX_LINE_IF_CHECKED;
        } else {
            return CHECKBOX_LINE_IF_UNCHECKED;
        }
    }

    private void logReadingExceptionAsError(IOException e) {
        log.error("I/O error occurred while reading " + getScriptFilename(), e);
    }

    private void logWritingExceptionAsError(IOException e) {
        log.error("I/O error occurred while writing " + getScriptFilename(), e);
    }

    private String getScriptFilename() {
        final Path path = Paths.get(this.absolutePathToScript);
        final String filename = path.getFileName().toString();
        return filename;
    }
}

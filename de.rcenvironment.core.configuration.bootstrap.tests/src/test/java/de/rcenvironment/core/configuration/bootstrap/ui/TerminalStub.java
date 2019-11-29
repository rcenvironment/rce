/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.input.KeyMappingProfile;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalSize;

/**
 * This is a terminal that can be used to test Lanterna UIs. It allows accessing the screen content. Furthermore, since this terminal is
 * only used in testing, it only supports moveCursor, putCharacter, readInput, getTerminalSize, clearScreen, and flush. Other methods are
 * not implemented and calls to them are ignored.
 *
 * @author Tobias Brieden
 * @author Alexander Weinert (renaming, documentation, minor refactoring)
 */
public class TerminalStub implements Terminal {

    private static final char SPACE = ' ';

    private TerminalSize size;

    // first index is the row index, second index is the column index, (0,0) is the upper left corner
    private char[][] content;

    private int cursorX;

    private int cursorY;

    private final Queue<Key> keyQueue = new LinkedList<Key>();

    /**
     * Contains a list of previous terminal content. Each time flush is called, the content is copied and the copy is added to this list.
     */
    private final LinkedList<char[][]> previousContent = new LinkedList<char[][]>();;

    private FlushListener listener;

    private Log log = LogFactory.getLog(TerminalStub.class);

    public TerminalStub(int columns, int rows) {

        this.size = new TerminalSize(columns, rows);
        this.content = new char[rows][columns];
    }

    @Override
    public synchronized Key readInput() {
        if (!keyQueue.isEmpty()) {
            log.debug("readInput");
            return keyQueue.poll();
        }

        return null;
    }

    /**
     * Adds a key to the internal input queue.
     * 
     * @param key The key to add.
     */
    public synchronized void appendKeyToQueue(Key key) {
        this.keyQueue.add(key);
    }

    /**
     * Adds a queue of keys to the internal input queue.
     * 
     * @param keys The keys to add.
     */
    public synchronized void appendKeyToQueue(Queue<Key> keys) {
        this.keyQueue.addAll(keys);
    }

    @Override
    public void applyBackgroundColor(Color arg0) {
        // ignore
    }

    @Override
    public void applyBackgroundColor(int arg0) {
        // ignore
    }

    @Override
    public void applyBackgroundColor(int arg0, int arg1, int arg2) {
        // ignore
    }

    @Override
    public void applyForegroundColor(Color arg0) {
        // ignore
    }

    @Override
    public void applyForegroundColor(int arg0) {
        // ignore
    }

    @Override
    public void applyForegroundColor(int arg0, int arg1, int arg2) {
        // ignore
    }

    @Override
    public void applySGR(SGR... arg0) {
        // ignore
    }

    @Override
    public synchronized void clearScreen() {
        for (int i = 0; i < content.length; i++) {
            for (int j = 0; j < content[i].length; j++) {
                content[i][j] = SPACE;
            }
        }
    }

    @Override
    public void enterPrivateMode() {
        log.debug("enterPrivateMode");
    }

    @Override
    public void exitPrivateMode() {
        log.debug("exitPrivateMode");
    }

    @Override
    public synchronized void flush() {
        log.debug("flush");
        for (char[] line : content) {
            log.debug(new String(line));
        }

        final char[][] copy = new char[content.length][];
        for (int i = 0; i < content.length; i++) {
            copy[i] = Arrays.copyOf(content[i], content[i].length);
        }
        previousContent.add(copy);

        if (listener != null) {
            listener.notifyListener();
        }
    }

    @Override
    public TerminalSize getTerminalSize() {
        return this.size;
    }

    @Override
    public synchronized void moveCursor(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
    }

    @Override
    public synchronized void putCharacter(char c) {

        this.content[cursorY][cursorX] = c;
        this.cursorX += 1;

        // TODO move to the next line, as soon as the end of the line is reached
    }

    @Override
    public TerminalSize queryTerminalSize() {
        return this.getTerminalSize();
    }

    @Override
    public void setCursorVisible(boolean arg0) {
        // ignore
    }

    @Override
    public void addInputProfile(KeyMappingProfile arg0) {
        // ignore
    }

    @Override
    public void addResizeListener(ResizeListener arg0) {
        // ignore
    }

    @Override
    public void removeResizeListener(ResizeListener arg0) {
        // ignore
    }

    public synchronized LinkedList<char[][]> getPreviousContent() {
        return this.previousContent;
    }

    /**
     * Creates a matcher that checks if the given expectedString is present in the screen content of the terminal.
     * 
     * @param expectedString The string to look for.
     * @return The matcher.
     */
    public static Matcher<char[][]> containsString(final String expectedString) {
        return new BaseMatcher<char[][]>() {

            @Override
            public boolean matches(Object uncastedContent) {
                final char[][] content = (char[][]) uncastedContent;

                // TODO do not check each line independently from one another but instead concatenate the lines while removing the
                // formatting

                for (char[] currentLine : content) {
                    String line = new String(currentLine);
                    if (line.contains(expectedString)) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("should contain ").appendValue(expectedString);
            }
        };
    }

    public void setFlushListener(FlushListener flushListener) {
        this.listener = flushListener;
    }

    /**
     * A FlushListener will be notified if flush is called on the terminal.
     *
     * @author Tobias Brieden
     */
    interface FlushListener {

        void notifyListener();
    };
}

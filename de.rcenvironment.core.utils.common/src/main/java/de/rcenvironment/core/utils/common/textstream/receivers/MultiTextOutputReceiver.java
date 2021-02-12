/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * This implementation of {@link TextOutputReceiver} forwards all received events to multiple other instances of {@link TextOutputReceiver}.
 * 
 * @author Tobias Rodehutskors
 * 
 */
public class MultiTextOutputReceiver implements TextOutputReceiver {

    private List<TextOutputReceiver> forwardTargets;

    public MultiTextOutputReceiver() {
        forwardTargets = new LinkedList<TextOutputReceiver>();
    }

    public MultiTextOutputReceiver(TextOutputReceiver firstTextOutputReceiver, TextOutputReceiver secondTextOutputReceiver) {
        this();
        forwardTargets.add(firstTextOutputReceiver);
        forwardTargets.add(secondTextOutputReceiver);
    }

    /**
     * Adds a new {@link TextOutputReceiver} to the list of receivers. If a {@link TextOutputReceiver} was already added to this
     * {@link MultiTextOutputReceiver}, it will not be added a second time.
     * 
     * @param newTextOutputReceiver The receiver to add.
     */
    public void addTextOutputReceiver(TextOutputReceiver newTextOutputReceiver) {
        if (!forwardTargets.contains(newTextOutputReceiver)) {
            forwardTargets.add(newTextOutputReceiver);
        }
    }

    /**
     * Removes a {@link TextOutputReceiver} from the list of receivers.
     * 
     * @param textOutputReceiverToRemove The receiver to remove;
     */
    public void removeTextOutputReceiver(TextOutputReceiver textOutputReceiverToRemove) {
        forwardTargets.remove(textOutputReceiverToRemove);
    }

    @Override
    public void onStart() {
        for (TextOutputReceiver forwardTarget : forwardTargets) {
            forwardTarget.onStart();
        }
    }

    @Override
    public void addOutput(String line) {
        for (TextOutputReceiver forwardTarget : forwardTargets) {
            forwardTarget.addOutput(line);
        }
    }

    @Override
    public void onFinished() {
        for (TextOutputReceiver forwardTarget : forwardTargets) {
            forwardTarget.onFinished();
        }
    }

    @Override
    public void onFatalError(Exception e) {
        for (TextOutputReceiver forwardTarget : forwardTargets) {
            forwardTarget.onFatalError(e);
        }
    }

}

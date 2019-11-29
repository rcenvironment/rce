/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A simple implementation of {@link TextOutputReceiver} that forwards all received events to
 * another {@link TextOutputReceiver} while prefixing each line with a given string.
 * 
 * @author Robert Mischke
 */
public class PrefixingTextOutForwarder implements TextOutputReceiver {

    private String prefix;

    private TextOutputReceiver forwardTarget;

    /**
     * @param prefix the prefix for each {@link #addOutput(String)} content line
     */
    public PrefixingTextOutForwarder(String prefix, TextOutputReceiver forwardTarget) {
        this.prefix = prefix;
        this.forwardTarget = forwardTarget;
    }

    @Override
    public void onStart() {
        forwardTarget.onStart();
    }

    @Override
    public void onFinished() {
        forwardTarget.onFinished();
    }

    @Override
    public void onFatalError(Exception e) {
        forwardTarget.onFatalError(e);
    }

    @Override
    public void addOutput(String line) {
        forwardTarget.addOutput(prefix + line);
    }

}

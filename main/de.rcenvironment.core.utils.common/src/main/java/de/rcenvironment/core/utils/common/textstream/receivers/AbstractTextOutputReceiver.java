/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * No-operation (NOP) implementation of {@link TextOutputReceiver} to avoid empty methods in
 * concrete implementations; subclasses only need to override the methods they use.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractTextOutputReceiver implements TextOutputReceiver {

    @Override
    public void onFinished() {}

    @Override
    public void onFatalError(Exception e) {}

    @Override
    public void onStart() {}

    @Override
    public void addOutput(String line) {}
}

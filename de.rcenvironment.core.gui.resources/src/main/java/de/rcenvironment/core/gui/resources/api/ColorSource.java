/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.jface.resource.ColorDescriptor;

/**
 * An abstract source of {@link ColorDescriptor}s.
 * 
 * @author Tobias Rodehutskors
 */
public interface ColorSource {

    /**
     * @return the {@link ColorDescriptor} of this color resource
     */
    ColorDescriptor getColorDescriptor();

}

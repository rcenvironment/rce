/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.spi;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentRevision;

/**
 * Listener for changes in the knowledge about published {@link ComponentDescription}s,
 * {@link ComponentRevision}s and {@link ComponentInstallation}s.
 * 
 * @author Robert Mischke
 */
public interface DistributedComponentKnowledgeListener {

    /**
     * Called on changes in the knowledge about published {@link ComponentDescription}s,
     * {@link ComponentRevision}s and {@link ComponentInstallation}s.
     * 
     * @param newState the new complete knowledge set
     */
    void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState);
}

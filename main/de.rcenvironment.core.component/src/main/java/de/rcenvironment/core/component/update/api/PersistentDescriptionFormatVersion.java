/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.update.api;

import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Constants for persistent component format versions. They are intended to be used in logical concatenation.
 * 
 * E.g., if a component updater has an update for persistent component format prior RCE 3.0.0 and has also one for
 * RCE 3.0.0, it must return in
 * {@link PersistentComponentDescriptionUpdater#getFormatVersionsAffectedByUpdate(String, boolean)}
 * {@link PersistentDescriptionFormatVersion}.PRIOR_VERSON_THREE |
 * {@link PersistentDescriptionFormatVersion}.FOR_VERSION_THREE.
 * If it has no update for any format version it must return {@link PersistentDescriptionFormatVersion}.NONE.
 * 
 * @author Doreen Seider
 */
public final class PersistentDescriptionFormatVersion {

    /** Workflow version < 3, i.e. RCE < 3.0.0. */
    public static final int BEFORE_VERSON_THREE = 1;
    
    /** Workflow version == 3, i.e. RCE == 3.0.0. */
    public static final int FOR_VERSION_THREE = 2;
    
    /** Workflow version > 3, i.e. RCE > 3.0.0. */
    public static final int AFTER_VERSION_THREE = 4;
    
    /** No update for any workflow format version available. */
    public static final int NONE = 0;
    
    private PersistentDescriptionFormatVersion() {}
    
}

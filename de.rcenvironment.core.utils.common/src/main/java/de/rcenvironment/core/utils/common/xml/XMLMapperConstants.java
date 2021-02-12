/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.common.xml;

/**
 * Constants related to {@link XMLMapperService}.
 * 
 * @author Doreen Seider
 *
 */
public final class XMLMapperConstants {
    
    /**
     * Global lock used to restrict the number of {@link Document} instances in the memory.
     */
    public static final Object GLOBAL_MAPPING_LOCK = new Object();
    
    private XMLMapperConstants() {}

}

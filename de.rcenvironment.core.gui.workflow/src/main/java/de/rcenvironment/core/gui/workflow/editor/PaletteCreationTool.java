/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.tools.CreationTool;


/**
 * This class extends the visibility of the getFactory() method of CreationTool to public.
 * @author Jan Flink
 *
 */
public class PaletteCreationTool extends CreationTool {

    /**
     * @param factory
     */
    public PaletteCreationTool(CreationFactory factory) {
        super(factory);
    }
    
    @Override
    public CreationFactory getFactory() {
        return super.getFactory();
    }
}

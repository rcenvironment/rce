/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.palette.view;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;

/**
 * Tests, if the palette view is shown.
 *
 * @author Jan Flink
 */
public class PaletteViewPropertyTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (property.equals("isPaletteViewShown") && !PlatformUI.getWorkbench().isClosing()) {
            return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .findView("de.rcenvironment.core.gui.palette.view.PaletteView") != null;
        }
        return false;
    }

}

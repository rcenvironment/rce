/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Component;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Discipline;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Named;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Parameter;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * The label provider for the parameter tree.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class ParameterTreeLabelProvider extends ColumnLabelProvider {

    private final Map<Class<?>, Image> imageCache = new Hashtable<Class<?>, Image>();

    public ParameterTreeLabelProvider(final Device device) {
        super();

        Image rootImage = ImageManager.getInstance().getSharedImage(StandardImages.VAMPZERO_ROOT);
        Image cImage = ImageManager.getInstance().getSharedImage(StandardImages.VAMPZERO_C);
        Image dImage = ImageManager.getInstance().getSharedImage(StandardImages.VAMPZERO_D);
        Image pImage = ImageManager.getInstance().getSharedImage(StandardImages.VAMPZERO_P);

        imageCache.put(ArrayList.class, rootImage);
        imageCache.put(Component.class, cImage);
        imageCache.put(Discipline.class, dImage);
        imageCache.put(Parameter.class, pImage);
    }

    @Override
    public String getText(final Object element) {
        if (element instanceof List<?>) {
            return "Parameters to apply";
        }
        if (element instanceof Named) {
            return ((Named) element).getName();
        }
        return null;
    }

    @Override
    public Image getImage(final Object element) {
        return imageCache.get(element.getClass()); // or null
    }

    @Override
    public void addListener(final ILabelProviderListener listener) {}

    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return false; // label not affected by property changes
    }

    @Override
    public void removeListener(final ILabelProviderListener listener) {}

    @Override
    public String getToolTipText(final Object element) {
        if (element instanceof Parameter) {
            return element.toString();
        }
        return null;
    }

}

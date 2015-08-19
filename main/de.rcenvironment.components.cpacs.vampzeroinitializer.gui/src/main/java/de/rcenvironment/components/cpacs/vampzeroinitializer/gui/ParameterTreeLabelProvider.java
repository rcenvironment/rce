/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import org.eclipse.swt.graphics.ImageData;

import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Component;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Discipline;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Named;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Parameter;

/**
 * The label provider for the parameter tree.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class ParameterTreeLabelProvider extends ColumnLabelProvider {

    private static final int ICON_SIZE_16 = 16;

    private final Map<Class<?>, Image> imageCache = new Hashtable<Class<?>, Image>();

    public ParameterTreeLabelProvider(final Device device) {
        super();
        imageCache.put(
            ArrayList.class,
            new Image(device, new ImageData(getClass().getClassLoader().getResourceAsStream("resources/icons/root.png")).scaledTo(
                ICON_SIZE_16, ICON_SIZE_16)));
        imageCache.put(Component.class,
            new Image(device, new ImageData(getClass().getClassLoader().getResourceAsStream("resources/icons/c16.png"))));
        imageCache.put(Discipline.class,
            new Image(device, new ImageData(getClass().getClassLoader().getResourceAsStream("resources/icons/d16.png"))));
        imageCache.put(Parameter.class,
            new Image(device, new ImageData(getClass().getClassLoader().getResourceAsStream("resources/icons/p16.png"))));
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
    public void dispose() {
        for (final Image image : imageCache.values()) {
            image.dispose();
        }
    }

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

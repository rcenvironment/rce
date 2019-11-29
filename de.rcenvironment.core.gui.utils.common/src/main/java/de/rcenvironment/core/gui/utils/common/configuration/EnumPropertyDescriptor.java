/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.lang.reflect.InvocationTargetException;

/**
 * A {@link org.eclipse.ui.views.properties.IPropertyDescriptor} representing an <code>enum</code>
 * valued property.
 * 
 * @author Christian Weiss
 */
public class EnumPropertyDescriptor extends SelectionPropertyDescriptor {

    public EnumPropertyDescriptor(final Class<? extends Enum<?>> enumType,
            final Object id, final String displayName) {
        super(id, displayName, new ValueProvider() {

            @Override
            public Object[] getValues() {
                return EnumPropertyDescriptor.getValues(enumType);
            }

        });
    }

    private static Object[] getValues(final Class<? extends Enum<?>> enumType) {
        try {
            return (Object[]) enumType.getMethod("values").invoke(null);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}

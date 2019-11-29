/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * The {@link ITreeContentProvider} for the {@link ConfigurationViewer}.
 * 
 * @author Christian Weiss
 */
public class ConfigurationViewerContentProvider implements ITreeContentProvider {

    /** The {@link ElementFactory} to create elements with. */
    private final ElementFactory elementFactory = new ElementFactory();

    /** The root element of the configuration tree. */
    private Element root;

    /** The {@link Viewer}s this content provider is providing content for. */
    private final Set<StructuredViewer> viewers = new HashSet<StructuredViewer>();

    /**
     * Adds a {@link Viewer} to the set of instances to be notified upon model changes.
     * 
     * @param viewer the {@link Viewer}
     */
    public void addViewer(final StructuredViewer viewer) {
        viewers.add(viewer);
    }

    /**
     * Removes a {@link Viewer} from the set of instances to be notified upon model changes.
     * 
     * @param viewer the {@link Viewer}
     */
    public void removeViewer(final StructuredViewer viewer) {
        viewers.remove(viewer);
    }

    /**
     * Gets the elements.
     * 
     * @param inputElement the input element
     * @return the elements {@inheritDoc}
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        final PropertyDescriptor descriptor = new PropertyDescriptor("<root>",
                "<root>");
        root = elementFactory.createElement(null, descriptor, inputElement);
        root.setOverrideValue(inputElement);
        return getChildren(root);
    }

    /**
     * Gets the parent.
     * 
     * @param element the element
     * @return the parent {@inheritDoc}
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    @Override
    public Object getParent(final Object element) {
        final Element treeElement = (Element) element;
        return treeElement.getParent();
    }

    /**
     * Checks for children.
     * 
     * @param element the element
     * @return true, if successful {@inheritDoc}
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    @Override
    public boolean hasChildren(final Object element) {
        final Element treeElement = (Element) element;
        if (treeElement instanceof Leaf) {
            return false;
        } else {
            return ((Node) treeElement).hasChildren();
        }
    }

    /**
     * Gets the children.
     * 
     * @param parentElement the parent element
     * @return the children {@inheritDoc}
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    @Override
    public Object[] getChildren(final Object parentElement) {
        final Element treeElement = (Element) parentElement;
        if (treeElement instanceof Leaf) {
            return new Object[0];
        } else {
            return ((Node) treeElement).getChildren();
        }
    }

    /**
     * Dispose.
     * 
     * {@inheritDoc}
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {
        // nothing to do
    }

    /**
     * Input changed.
     * 
     * @param viewer the viewer
     * @param oldInput the old input
     * @param newInput the new input {@inheritDoc}
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
     *      java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
    }

    /**
     * Fire element changed.
     * 
     * @param element the element
     */
    private void fireElementChanged(final Element element) {
        for (final StructuredViewer viewer : viewers) {
            try {
                viewer.refresh(element);
            } catch (RuntimeException e) {
                e = null;
            }
        }
    }

    /**
     * An element in the tree.
     * 
     * @author Christian Weiss
     */
    /* default */abstract class Element {

        /** The parent. */
        private final Node parent;

        /** The descriptor. */
        private final IPropertyDescriptor descriptor;

        /** The override value. */
        private Object overrideValue;

        /**
         * Instantiates a new element.
         * 
         * @param parent the parent
         * @param descriptor the descriptor
         */
        private Element(final Node parent, final IPropertyDescriptor descriptor) {
            this.parent = parent;
            this.descriptor = descriptor;
        }

        /**
         * Sets the override value.
         * 
         * @param overrideValue the override value
         */
        public void setOverrideValue(final Object overrideValue) {
            this.overrideValue = overrideValue;
        }

        /**
         * Initializes this {@link Element}.
         * 
         */
        protected void initialize() {
            if (parent == null) {
                return;
            }
            PropertyChangeListener listener = new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    Element element = Element.this;
                    fireElementChanged(element);
                }

            };
            try {
                final Object bean = parent.getValue();
                final Method addMethod =
                    bean.getClass().getMethod("addPropertyChangeListener", String.class, PropertyChangeListener.class);
                addMethod.invoke(bean, descriptor.getId().toString(), listener);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                e = null;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }

        }

        /**
         * Returns the parent.
         * 
         * @return the parent
         */
        public Node getParent() {
            return parent;
        }

        /**
         * Returns the display name.
         * 
         * @return the display name
         */
        public String getDisplayName() {
            return descriptor.getDisplayName();
        }

        /**
         * Returns the display value.
         * 
         * @return the display value
         */
        public String getDisplayValue() {
            return descriptor.getLabelProvider().getText(getValue());
        }

        /**
         * Returns the description.
         * 
         * @return the description
         */
        public String getDescription() {
            return descriptor.getDescription();
        }

        /**
         * Returns the {@link IPropertyDescriptor}.
         * 
         * @return the {@link IPropertyDescriptor}
         */
        public IPropertyDescriptor getPropertyDescriptor() {
            return descriptor;
        }

        /**
         * Returns the value.
         * 
         * @return the value
         */
        protected Object getValue() {
            if (overrideValue != null) {
                return overrideValue;
            }
            return parent.getValue(descriptor.getId());
        }

        /**
         * Sets the value.
         * 
         * @param value the new value
         */
        protected void setValue(final Object value) {
            if (overrideValue != null) {
                throw new UnsupportedOperationException();
            }
            parent.setValue(descriptor.getId(), value);
        }

        /**
         * Creates the property editor.
         * 
         * @param editorParent the editor parent
         * @return the cell editor
         */
        public CellEditor createPropertyEditor(final Composite editorParent) {
            return descriptor.createPropertyEditor(editorParent);
        }

    }

    /**
     * An element of the tree having no sub-elements, usually a simple property (scalar value).
     */
    /* package */final class Leaf extends Element {

        /**
         * Instantiates a new leaf.
         * 
         * @param parent the parent
         * @param descriptor the descriptor
         */
        private Leaf(final Node parent, final IPropertyDescriptor descriptor) {
            super(parent, descriptor);
        }

    }

    /**
     * An element of the tree having sub-elements, usually a complex property (composite value).
     */
    /* package */final class Node extends Element {

        /** The source. */
        private final IConfigurationSource source;

        /**
         * Instantiates a new node.
         * 
         * @param parent the parent
         * @param descriptor the descriptor
         * @param source the source
         */
        private Node(final Node parent, final IPropertyDescriptor descriptor,
                final IConfigurationSource source) {
            super(parent, descriptor);
            this.source = source;
        }

        /**
         * Gets the display value.
         * 
         * @return the display value {@inheritDoc}
         * @see de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerContentProvider.Element#getDisplayValue()
         */
        @Override
        public String getDisplayValue() {
            final Object editableValue = source.getEditableValue();
            if (editableValue instanceof String) {
                return editableValue.toString();
            } else {
                return super.getDisplayValue();
            }
        }

        /**
         * Returns the value of the property with the given id.
         * 
         * @param id the id
         * @return the value
         */
        private Object getValue(final Object id) {
            return source.getPropertyValue(id);
        }

        /**
         * Sets the value of the property with the given id to the given value.
         * 
         * @param id the id
         * @param value the value
         */
        private void setValue(final Object id, final Object value) {
            source.setPropertyValue(id, value);
        }

        /**
         * Checks for children.
         * 
         * @return true, if successful
         */
        public boolean hasChildren() {
            return true;
        }

        /**
         * Returns the children.
         * 
         * @return the children
         */
        public Element[] getChildren() {
            final List<Element> children = new LinkedList<Element>();
            final IPropertyDescriptor[] descriptors = source
                    .getConfigurationPropertyDescriptors();
            for (final IPropertyDescriptor descriptor : descriptors) {
                final Element child;
                Object value = source
                        .getPropertyValue(descriptor.getId());
                // wrap arrays
                if (value != null && value.getClass().getComponentType() != null) {
                    value = new ArraySource<Object>() {

                        @Override
                        public Object[] getValue() {
                            return (Object[]) source.getPropertyValue(descriptor.getId());
                        }

                        @Override
                        public void setValue(Object[] value) {
                            source.setPropertyValue(descriptor.getId(), value);
                        }

                    };
                }
                child = elementFactory.createElement(this, descriptor, value);
                if (child == null) {
                    continue;
                }
                children.add(child);
            }
            Collections.sort(children, new Comparator<Element>() {

                @Override
                public int compare(Element o1, Element o2) {
                    return o1.getDisplayName().toLowerCase()
                            .compareTo(o2.getDisplayName().toLowerCase());
                }
            });
            return children.toArray(new Element[0]);
        }

    }

    /**
     * A factory to create {@link Element}s based on {@link IPropertyDescriptor}s of configuration
     * properties.
     */
    protected final class ElementFactory {

        private ElementFactory() {}
        
        /**
         * Creates a new Element object.
         * 
         * @param parent the parent
         * @param descriptor the descriptor
         * @param value the value
         * @return the element
         */
        private Element createElement(final Node parent,
                final IPropertyDescriptor descriptor, final Object value) {
            final Element result;
            if (descriptor.getClass() != PropertyDescriptor.class) {
                result = new Leaf(parent, descriptor);
            } else {
                final IConfigurationSource source = (IConfigurationSource) AdapterManager
                        .getInstance().getAdapter(value, IConfigurationSource.class);
                if (source == null) {
                    result = new Leaf(parent, descriptor);
                } else {
                    result = new Node(parent, descriptor, source);
                }
            }
            result.initialize();
            return result;
        }

    }

    /**
     * The Interface ArraySource.
     * 
     * @param <T> the generic type
     */
    public interface ArraySource<T> {

        /**
         * Gets the value.
         * 
         * @return the value
         */
        T[] getValue();

        /**
         * Sets the value.
         * 
         * @param value the new value
         */
        void setValue(T[] value);

    }

}

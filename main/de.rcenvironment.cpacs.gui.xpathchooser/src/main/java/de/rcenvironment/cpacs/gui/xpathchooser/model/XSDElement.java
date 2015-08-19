/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.xpathchooser.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simplified representation of an XSD element. An XSD element only has a name, sub-Elements and/or Attributes. All other XSD elements are
 * removed.
 * 
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XSDElement extends AbstractXSDValueHolder {

    /**
     * Reuse.
     */
    public static final String[] EMPTY_STRING = new String[0];

    /**
     * Inheritable.
     */
    private static final long serialVersionUID = -6618584152825821109L;

    /**
     * List of subelements.
     */
    private List<XSDElement> elements = new ArrayList<XSDElement>();

    /**
     * List of attributes.
     */
    private List<XSDAttribute> attributes = new ArrayList<XSDAttribute>();

    /**
     * Contains the currently selected attribute (or custom value).
     */
    private String idAttribute = null;

    /**
     * The currently selected attribute value.
     */
    private String idValue = null;

    /**
     * Contains the names of all uid attribute values in the document (or custom values).
     */
    private Map<String, Set<String>> idValues = new Hashtable<String, Set<String>>();

    /**
     * Construct a new XSDElement by name and parent.
     * 
     * @param name The name to set.
     * @param parent The parent to set.
     */
    public XSDElement(XSDElement parent, String name) {
        super(parent, name);
    }

    /**
     * Returns the elements.
     * 
     * @return Returns the elements.
     */
    public List<XSDElement> getElements() {
        return elements;
    }

    /**
     * The elements to set.
     * 
     * @param theElements The elements to set.
     */
    public void setElements(final List<XSDElement> theElements) {
        elements = theElements;
    }

    /**
     * Returns the attributes.
     * 
     * @return Returns the attributes.
     */
    public List<XSDAttribute> getAttributes() {
        return attributes;
    }

    /**
     * The attributes to set.
     * 
     * @param theAttributes The attributes to set.
     */
    public void setAttributes(final List<XSDAttribute> theAttributes) {
        attributes = theAttributes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.cpacs.gui.xpathchooser.model.XSDTreeItem#getPath()
     */
    @Override
    public String getPath() {
        if (getParent() == null) {
            return "/" + name;
        }
        return getParent().getPath() + "/" + name;
    }

    /**
     * Get the currently set attribute name.
     * 
     * @return The attribute name from the document or a custom value
     */
    public String getCurrentAttributeName() {
        return idAttribute;
    }

    /**
     * GEt the currently set attribute value.
     * 
     * @return The attribute value from the document or a custom value
     */
    public String getCurrentAttributeValue() {
        return idValue;
    }

    /**
     * Set the current attribute name.
     * 
     * @param value The value to set
     */
    public void setCurrentAttributeName(final String value) {
        idAttribute = value.intern();
    }

    /**
     * Set the current attribute value.
     * 
     * @param value The value to set
     */
    public void setCurrentAttributeValue(final String value) {
        idValue = value.intern();
    }

    /**
     * Get all available attribute names for the selected element in the document. If a custom name was provided by the user, it is included
     * in the return array.
     * 
     * @return The set of all attribute names in the document plus the user input
     */
    public String[] getAttributeNames() {
        final Set<String> names = new HashSet<String>();
        names.addAll(idValues.keySet());
        if (idAttribute != null) {
            names.add(idAttribute);
        }
        return names.toArray(EMPTY_STRING);
    }

    /**
     * Get all values for the currently set attribute name in the document. If a custom value was provided by the user, it is included in
     * the return array.
     * 
     * @return The set of all attribute values in the document plus the user input, or just the user input if the attribute name is also
     *         custom
     */
    public String[] getAttributeValues() {
        if (idValues.containsKey(idAttribute)) {
            final Set<String> values = new HashSet<String>();
            values.addAll(idValues.get(idAttribute));
            if (idValue != null) {
                values.add(idValue);
            }
            return values.toArray(EMPTY_STRING);
        }
        return new String[] { idValue };
    }

    /**
     * When modifying the model, this needs to be set here.
     * 
     * @param name The new name to set
     */
    public void setAttributeName(final String name) {
        if ((name == null) || name.equals("")) {
            setCurrentAttributeName("");
            return;
        }
        if (!idValues.containsKey(name)) {
            idValues.put(name, new LinkedHashSet<String>());
        }
        setCurrentAttributeName(name);
        // always add "current value" to values when changing attribute name first
        if ((idValue != null) && !idValue.equals("")) {
            idValues.get(idAttribute).add(idValue);
        }
    }

    /**
     * When modifying the model, this needs to be set here.
     * 
     * @param value The value to set for the current attribute name
     */
    public void setAttributeValue(final String value) {
        if ((value == null) || value.equals("")) {
            setCurrentAttributeValue("");
            return;
        }
        if (!idValues.containsKey(idAttribute)) {
            idValues.put(idAttribute, new LinkedHashSet<String>());
        }
        setCurrentAttributeValue(value);
        idValues.get(idAttribute).add(idValue);
    }

    /**
     * When lazily creating meta data, we need to set all allowed values on the attribute.
     * 
     * @param name The attribute name
     * @param values The allowed values
     */
    public void setAttributeValues(final String name, final Set<String> values) {
        if (!idValues.containsKey(name)) {
            idValues.put(name, new HashSet<String>());
        }
        idValues.get(name).addAll(values);
    }

}

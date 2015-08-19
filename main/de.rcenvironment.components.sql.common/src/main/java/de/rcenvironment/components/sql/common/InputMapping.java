/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import de.rcenvironment.components.sql.common.InputMapping.ColumnMapping;

/**
 * Input mapping.
 * 
 * @author Christian Weiss
 */
public class InputMapping implements Serializable, Iterable<ColumnMapping> {

    private static final int NO_SUCH_ELEMENT_INDEX = -1;

    private static final String SEPARATOR_LINE = ";";

    private static final String SEPARATOR_COLUMN = "#";

    private static final long serialVersionUID = 3828775084678624476L;

    private static final Pattern SQL_NAME_PATTERN = Pattern.compile("^[_a-zA-Z0-9]*$");

    private final List<ColumnMapping> columnMappings = new LinkedList<ColumnMapping>();

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private final PropertyChangeListener columnMappingsListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            final String columnMappingPropertyName = event.getPropertyName();
            final String propertyName = String.format("columnMapping.%s", columnMappingPropertyName);
            final ColumnMapping columnMapping = (ColumnMapping) event.getSource();
            final int index = indexOf(columnMapping);
            if (index >= 0) {
                changeSupport.fireIndexedPropertyChange(propertyName, index, event.getOldValue(), event.getNewValue());
            }
        }

    };

    private final VetoableChangeListener columnMappingsVeto = new VetoableChangeListener() {

        @Override
        public void vetoableChange(final PropertyChangeEvent event) throws PropertyVetoException {
            final ColumnMapping columnMapping = (ColumnMapping) event.getSource();
            final String propertyName = event.getPropertyName();
            if (ColumnMapping.PROPERTY_NAME.equals(propertyName)) {
                final String name = (String) event.getNewValue();
                if (name != null && !name.isEmpty()) {
                    if (!SQL_NAME_PATTERN.matcher(name).matches()) {
                        throw new PropertyVetoException(String.format("Not a valid name ('%s').", name), event);
                    }
                    for (final ColumnMapping otherColumnMapping : columnMappings) {
                        if (columnMapping == otherColumnMapping) {
                            continue;
                        }
                        final String otherName = otherColumnMapping.getName();
                        if (name != null && !name.isEmpty() && name.equals(otherName)) {
                            throw new PropertyVetoException("Duplicate names are not allowed.", event);
                        }
                    }
                }
            }
        }

    };

    /**
     * Adds a {@link ColumnMapping}.
     * 
     * @return a new {@link ColumnMapping}
     */
    public ColumnMapping add() {
        return add(columnMappings.size());
    }

    /**
     * Adds a {@link ColumnMapping} with the specified name and type.
     * 
     * @param name the name
     * @param type the type
     * @return the new {@link ColumnMapping} with the specified name and type
     */
    public ColumnMapping add(final String name, final ColumnType type) {
        final ColumnMapping columnMapping = add();
        columnMapping.setName(name);
        columnMapping.setType(type);
        return columnMapping;
    }

    /**
     * Adds a {@link ColumnMapping} at the specified index.
     * 
     * @param index the position index
     * @return the new {@link ColumnMapping} at the specified index
     */
    public ColumnMapping add(final int index) {
        final ColumnMapping columnMapping = new ColumnMapping(index);
        columnMapping.addVetoableChangeListener(columnMappingsVeto);
        columnMapping.addPropertyChangeListener(columnMappingsListener);
        columnMappings.add(index, columnMapping);
        updateIndexs(index);
        changeSupport.fireIndexedPropertyChange("columnMapping", index, null, columnMapping);
        return columnMapping;
    }

    /**
     * Returns the {@link ColumnMapping} at the specified index.
     * 
     * @param index the index
     * @return the {@link ColumnMapping} at the specified index
     */
    public ColumnMapping get(final int index) {
        return columnMappings.get(index);
    }

    /**
     * Removes the given {@link ColumnMapping}.
     * 
     * @param columnMapping the {@link ColumnMapping} to remove
     */
    public void remove(final ColumnMapping columnMapping) {
        final int index = columnMappings.indexOf(columnMapping);
        remove(index);
    }

    /**
     * Removes the {@link ColumnMapping} at the given index.
     * 
     * @param index the index of the {@link ColumnMapping} to remove
     */
    public void remove(final int index) {
        final ColumnMapping oldValue = columnMappings.get(index);
        oldValue.addVetoableChangeListener(columnMappingsVeto);
        oldValue.removePropertyChangeListener(columnMappingsListener);
        columnMappings.remove(index);
        updateIndexs(index);
        ColumnMapping newValue;
        if (index < columnMappings.size()) {
            newValue = columnMappings.get(index);
        } else {
            newValue = null;
        }
        // to enforce PropertyChangeEvent
        if (newValue != null && newValue.equals(oldValue)) {
            newValue = null;
        }
        changeSupport.fireIndexedPropertyChange("columnMapping", index, oldValue, newValue);
    }

    protected void updateIndexs(final int index) {
        // update index attributes
        for (int i = 0; i < columnMappings.size(); ++i) {
            columnMappings.get(i).setIndex(i);
        }
    }

    /**
     * Returns the number of {@link ColumnMappings} contained in this {@link InputMapping}.
     * 
     * @return the number of {@link ColumnMappings}
     */
    public int size() {
        return columnMappings.size();
    }

    /**
     * Returns, whether the specified {@link ColumnMapping} is contained.
     * 
     * @param columnMapping the {@link ColumnMapping}
     * @return true, if the specified {@link ColumnMapping} is contained
     */
    public boolean contains(final ColumnMapping columnMapping) {
        return columnMappings.contains(columnMapping);
    }

    /**
     * Returns the index of the given {@link ColumnMapping}.
     * 
     * @param columnMapping the {@link ColumnMapping}
     * @return the index of the given {@link ColumnMapping}
     */
    public int indexOf(final ColumnMapping columnMapping) {
        int index = NO_SUCH_ELEMENT_INDEX;
        // try reference equality first, as 'empty' instances are 'equal(...)' to each other
        for (int i = 0; i < columnMappings.size(); ++i) {
            if (columnMapping == columnMappings.get(i)) {
                index = i;
                break;
            }
        }
        // use instance equality (equals(...)-method as backup (should never be used!)
        if (index == NO_SUCH_ELEMENT_INDEX) {
            index = columnMappings.indexOf(columnMapping);
        }
        return index;
    }

    /**
     * Moves the specified {@link ColumnMapping} one position up.
     * 
     * @param columnMapping the {@link ColumnMapping} to move up
     */
    public void moveUp(final ColumnMapping columnMapping) {
        final int index = columnMappings.indexOf(columnMapping);
        if (index < 0) {
            throw new IllegalArgumentException();
        }
        if (index > 0) {
            columnMappings.remove(index);
            columnMappings.add(index - 1, columnMapping);
            updateIndexs(index);
        }
    }

    /**
     * Moves the specified {@link ColumnMapping} one position down.
     * 
     * @param columnMapping the {@link ColumnMapping} to move down
     */
    public void moveDown(final ColumnMapping columnMapping) {
        final int index = columnMappings.indexOf(columnMapping);
        if (index < 0) {
            throw new IllegalArgumentException();
        }
        if (index < columnMappings.size() - 1) {
            columnMappings.remove(index);
            columnMappings.add(index + 1, columnMapping);
            updateIndexs(index);
        }
    }

    public ColumnMapping[] getColumnMappingsArray() {
        return columnMappings.toArray(new ColumnMapping[columnMappings.size()]);
    }

    /**
     * Deserializes a {@link InputMapping} from a <code>String</code>.
     * 
     * @param string the serialized {@link InputMapping}
     * @return the deserialized {@link InputMapping}
     */
    public static InputMapping deserialize(final String string) {
        final InputMapping result = new InputMapping();
        try (final Scanner scanner = new Scanner(string)) {
            scanner.useDelimiter(SEPARATOR_LINE);
            while (scanner.hasNext()) {
                final String line = scanner.next();
                if (line.isEmpty()) {
                    break;
                }
                final String[] values = line.split(SEPARATOR_COLUMN);
                if (values.length == 0) {
                    if (line.trim().matches(SEPARATOR_COLUMN)) {
                        result.add();
                    }
                    continue;
                }
                final String name;
                if (values.length >= 1) {
                    name = values[0];
                } else {
                    name = null;
                }
                final ColumnType type;
                if (values.length >= 2) {
                    final String typeString = values[1];
                    type = ColumnType.valueOf(typeString);
                } else {
                    type = null;
                }
                if (name == null && type == null) {
                    continue;
                }
                final ColumnMapping columnMapping = result.add();
                columnMapping.setName(name);
                columnMapping.setType(type);
            }
        }
        return result;
    }

    /**
     * Returns a serialized <code>String</code> representing this {@link InputMapping}.
     * 
     * @return the serialized {@link InputMapping}
     */
    public String serialize() {
        final StringBuilder builder = new StringBuilder();
        final ColumnMapping[] columnMappingsArray = columnMappings.toArray(new ColumnMapping[0]);
        for (int index = 0; index < columnMappingsArray.length; ++index) {
            if (index > 0) {
                builder.append(SEPARATOR_LINE);
            }
            final ColumnMapping columnMapping = columnMappingsArray[index];
            String name = columnMapping.getName();
            if (name == null) {
                name = "";
            }
            builder.append(name);
            builder.append(SEPARATOR_COLUMN);
            final ColumnType type = columnMapping.getType();
            final String typeString;
            if (type != null) {
                typeString = type.name();
            } else {
                typeString = "";
            }
            builder.append(typeString);
        }
        final String result = builder.toString();
        return result;
    }

    @Override
    public Iterator<ColumnMapping> iterator() {
        return columnMappings.iterator();
    }

    /**
     * Adds a {@link PropertyChangeListener}.
     * 
     * @param listener the {@link PropertyChangeListener} to add
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@link PropertyChangeListener}.
     * 
     * @param listener the {@link PropertyChangeListener} to remove
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public boolean equals(Object obj) {
        final boolean result;
        if (obj instanceof InputMapping) {
            final InputMapping other = (InputMapping) obj;
            result = columnMappings.equals(other.columnMappings);
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return columnMappings.hashCode();
    }

    /**
     * Column mapping.
     * 
     * @author Christian Weiss
     */
    public class ColumnMapping implements Serializable {

        private static final String PROPERTY_NAME = "name";

        private static final String PROPERTY_TYPE = "type";

        private static final long serialVersionUID = 5688799529082981800L;

        private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

        private final VetoableChangeSupport vetoSupport = new VetoableChangeSupport(this);

        private int index;

        private String name;

        private ColumnType type;

        public ColumnMapping(final int index) {
            this(index, "", ColumnType.IGNORE);
        }

        public ColumnMapping(final int index, final String name, final ColumnType type) {
            this.index = index;
            // setter can be used as no veto or change listeners can be registered
            setName(name);
            setType(type);
        }

        private void setIndex(final int index) {
            if (this.index != index) {
                this.index = index;
            }
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         * 
         * @param name the name
         */
        public void setName(final String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }
            final String oldValue = this.name;
            try {
                vetoSupport.fireVetoableChange(PROPERTY_NAME, oldValue, name);
                this.name = name;
                changeSupport.firePropertyChange(PROPERTY_NAME, oldValue, name);
            } catch (PropertyVetoException e) {
                e = null;
            }
        }

        public ColumnType getType() {
            return type;
        }

        /**
         * Sets the type.
         * 
         * @param type the type
         */
        public void setType(final ColumnType type) {
            if (type == null) {
                throw new IllegalArgumentException();
            }
            final ColumnType oldValue = this.type;
            try {
                vetoSupport.fireVetoableChange(PROPERTY_TYPE, oldValue, type);
                this.type = type;
                changeSupport.firePropertyChange(PROPERTY_TYPE, oldValue, type);
            } catch (PropertyVetoException e) {
                e = null;
            }
        }

        /**
         * Adds a {@link PropertyChangeListener}.
         * 
         * @param listener the {@link PropertyChangeListener} to add
         */
        public void addPropertyChangeListener(final PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
        }

        /**
         * Removes a {@link PropertyChangeListener}.
         * 
         * @param listener the {@link PropertyChangeListener} to remove
         */
        public void removePropertyChangeListener(final PropertyChangeListener listener) {
            changeSupport.removePropertyChangeListener(listener);
        }

        /**
         * Adds a {@link VetoableChangeListener}.
         * 
         * @param listener the {@link VetoableChangeListener} to add
         */
        public void addVetoableChangeListener(final VetoableChangeListener listener) {
            vetoSupport.addVetoableChangeListener(listener);
        }

        /**
         * Removes a {@link VetoableChangeListener}.
         * 
         * @param listener the {@link VetoableChangeListener} to remove
         */
        public void removeVetoableChangeListener(final VetoableChangeListener listener) {
            vetoSupport.removeVetoableChangeListener(listener);
        }

        @Override
        public boolean equals(final Object obj) {
            boolean result;
            if (obj instanceof ColumnMapping) {
                final ColumnMapping other = (ColumnMapping) obj;
                result = getIndex() == other.getIndex() && name.equals(other.name) && type.equals(other.type);
            } else {
                result = false;
            }
            return result;
        };

        @Override
        public int hashCode() {
            final int result;
            result = name.hashCode() + type.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final String typeString;
            if (type != null) {
                typeString = type.getLabel();
            } else {
                typeString = null;
            }
            return String.format("%s (%s)", name, typeString);
        }

    }

}

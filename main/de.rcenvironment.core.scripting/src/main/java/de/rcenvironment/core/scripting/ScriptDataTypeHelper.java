/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;

/**
 * Helper clss for data type related methods in all scripting bundles. Used for e.g. converting a
 * given {@link TypedDatum} into an java object.
 * 
 * @author Sascha Zur
 */
public final class ScriptDataTypeHelper {

    @Deprecated
    private ScriptDataTypeHelper() {

    }

    /**
     * Reads the value of a given {@link TypedDatum} and returns it as a java {@link Object}. Used
     * for Python and Jython scripts.
     * 
     * @param typedDatumOfCell to read
     * @return Object containing the value in its correct java type
     */
    public static Object getObjectOfEntryForPythonOrJython(TypedDatum typedDatumOfCell) {
        Object returnValue = null;
        if (typedDatumOfCell == null || typedDatumOfCell.getDataType() == DataType.Empty) {
            return "None";
        }
        switch (typedDatumOfCell.getDataType()) {
        case Boolean:
            boolean bool = (((BooleanTD) typedDatumOfCell).getBooleanValue());
            if (bool) {
                returnValue = "True";
            } else {
                returnValue = "False";
            }
            break;
        case ShortText:
            returnValue = StringEscapeUtils.escapeJava(((ShortTextTD) typedDatumOfCell).getShortTextValue());
            break;
        case Integer:
            returnValue = ((IntegerTD) typedDatumOfCell).getIntValue();
            break;
        case Float:
            returnValue = ((FloatTD) typedDatumOfCell).getFloatValue();
            break;
        default:
            returnValue = typedDatumOfCell.toString();
            break;
        }
        return returnValue;
    }

    /**
     * Returns a {@link TypedDatum} that fits the java class of the given value.
     * 
     * @param value to create the TypedDatum from
     * @param typedDatumFactory :
     * @return :
     * */
    public static TypedDatum getTypedDatum(Object value, TypedDatumFactory typedDatumFactory) {
        TypedDatum returnValue = null;
        if (value == null) {
            returnValue = typedDatumFactory.createEmpty();
        } else if (value.getClass().equals(Integer.class)) {
            returnValue = typedDatumFactory.createInteger((Integer) value);
        } else if (value.getClass().equals(Long.class)) {
            returnValue = typedDatumFactory.createInteger((Long) value);
        } else if (value.getClass().equals(Double.class)) {
            returnValue = typedDatumFactory.createFloat((Double) value);
        } else if (value.getClass().equals(Float.class)) {
            returnValue = typedDatumFactory.createFloat((Float) value);
        } else if (value.getClass().equals(String.class)) {
            returnValue = typedDatumFactory.createShortText((String) value);
        } else if (value.getClass().equals(Boolean.class)) {
            returnValue = typedDatumFactory.createBoolean((Boolean) value);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) value;
            boolean allFloat = true;
            for (Object o : list) {
                if (!(o instanceof Double || o instanceof Float || o instanceof Integer)) {
                    allFloat = false;
                }
            }
            if (allFloat) {
                FloatTD[] values = new FloatTD[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Double) {
                        values[i] = typedDatumFactory.createFloat((Double) list.get(i));
                    } else {
                        values[i] = typedDatumFactory.createFloat((Integer) list.get(i));
                    }
                }
                returnValue = typedDatumFactory.createVector(values);
            }
        } else {
            returnValue = typedDatumFactory.createShortText(value.toString());
        }
        return returnValue;
    }
}

/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.common.StringUtils;


/**
 * Helper class for data type related methods in all scripting bundles. Used for converting a given {@link TypedDatum} into a java
 * object, as well as the other way around.
 * 
 * @author Sascha Zur
 * @author Martin Misiak
 */
public final class ScriptDataTypeHelper {

    private static final int LONG_BIT_LENGTH = 64;
    @Deprecated
     private ScriptDataTypeHelper() {

    }
    
    /**
     * Reads the value of a given {@link TypedDatum} and returns it as a java {@link Object}. Used for Python and Jython scripts.
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
     * Helper method for parseTypedDatum for list types(VectorTD, MatrixTD, SmallTableTD).
     * @param value to create the TypedDatum from.
     * @param typedDatumFactory
     * @param requiredType : The {@link DataType} the value should be parsed to.
     * @return Returns null if no corresponding {@link TypedDatum} was found.
     */
    private static TypedDatum parseListValue(Object value, TypedDatumFactory typedDatumFactory, DataType requiredType)
        throws ComponentException{
        
        if (!(value instanceof List)) {
            throw new ComponentException(StringUtils.format("Output value '%s' can not be parsed to an array based"
                + " data type like Vector, Matrix or SmallTable.",
                value.toString()));
        }
        
        
        TypedDatum returnValue = null;
        boolean matrixOrSmalltable = true;
        
        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) value;

        if (list.size() == 0){
            throw new ComponentException(StringUtils.format("Output value '%s' can not be parsed to an array based"
                    + " data type like Vector, Matrix or SmallTable, since it contains no elements.",
                    value.toString()));
        }
        
        if (requiredType == DataType.Vector) {
            FloatTD[] values = new FloatTD[list.size()];
            for (int i = 0; i < list.size(); i++) {
                FloatTD resolvedValue = resolveValueToFloatTD(list.get(i), typedDatumFactory);
                if (resolvedValue == null) {
                    throw new ComponentException(StringUtils.format("Failed to parse output value '%s' to data type Vector.",
                        value.toString()));
                }
                values[i] = resolvedValue;
            }
            returnValue = typedDatumFactory.createVector(values);
            matrixOrSmalltable = false;
        } else {
            
        
            for (Object o : list) {
                if (!(o instanceof List)) {
                    matrixOrSmalltable = false;
                    throw new ComponentException(StringUtils.format("Output value '%s' can not be parsed to a 2-dimensional "
                       + " data type, like Matrix or SmallTable, since its elements must be stored as an array of arrays. However '%s'"
                       + "is not an array. ",
                        value.toString(), o.toString()));
                } else {
                    @SuppressWarnings("unchecked") List<Object> tempList = (List<Object>) o;
                    if (tempList.size() == 0) {
                        throw new ComponentException(StringUtils.format("Output value '%s' can not be parsed to a"
                            + " Matrix or SmallTable, since some rows contain no elements.",
                            value.toString()));
                    }
                }
            }
        }
        if (matrixOrSmalltable) {
            @SuppressWarnings("unchecked")
            int columnDimension = ((List<Object>) list.get(0)).size();
            TypedDatum[][] values = new TypedDatum[list.size()][columnDimension];
            FloatTD[][] matrixValues = new FloatTD[list.size()][columnDimension]; 
            boolean isMatrix = (requiredType == DataType.Matrix);
            boolean isSmallTable = (requiredType == DataType.SmallTable);
            
            for (int i = 0; i < list.size(); i++) {
                @SuppressWarnings("unchecked") List<Object> row = (List<Object>) list.get(i);
                if (row.size() != columnDimension) {
                    throw new ComponentException(StringUtils.format("Output value '%s' can not be parsed to a 2-dimensional "
                        + "data type, like Matrix or SmallTable, since the individual row dimensions are not constant.",
                         value.toString()));
                }
                
                for (int j = 0; j < row.size(); j++) {
                    
                    if (isMatrix) {
                        matrixValues[i][j] = resolveValueToFloatTD(row.get(j), typedDatumFactory);
                        if (matrixValues[i][j] == null) {
                            throw new ComponentException(StringUtils.format("Output value '%s' can not be parsed to data type Matrix."
                                + "The value of one of its cells (%s, %s) could not be parsed to a float.", value.toString(), i, j));
                        }
                    } else if (isSmallTable) {
                        values[i][j] = resolveSimpleValue(row.get(j), typedDatumFactory);
                        if (values[i][j] == null) {
                            throw new ComponentException(StringUtils.format("Output value '%s' can not be parsed to data type SmallTable."
                                + " The value of one of its cells (%s, %s) could not be parsed to a valid datatype.",
                                value.toString(), i, j));
                        }
                    }
                    
                }
            }
            
            if (isMatrix) {
                returnValue = typedDatumFactory.createMatrix(matrixValues);
            } else if (isSmallTable) {
                returnValue = typedDatumFactory.createSmallTable(values);
            }
        }
        
        return returnValue;
    }
    
    /**
     * 
     * This method tries to resolve all Java integer formats into {@link IntegerTD}.
     * 
     * @param value to create the {@link IntegerTD} from
     * @param typedDatumFactory 
     * @return Returns null if the value object can not be resolved to {@link IntegerTD}.
     */
    private static TypedDatum resolveValueToIntegerTD(Object value, TypedDatumFactory typedDatumFactory){
        
        TypedDatum returnValue = null;
        if (value == null) {
            return null;
        } else if (value.getClass().equals(Integer.class)) {
            returnValue = typedDatumFactory.createInteger((Integer) value);
        } else if (value.getClass().equals(Long.class)) {
            returnValue = typedDatumFactory.createInteger((Long) value);
        } else if (value.getClass().equals(BigInteger.class)) {
            BigInteger newValue = (BigInteger) value;
            if (newValue.bitLength() >= LONG_BIT_LENGTH) {
                returnValue = null;
            } else {
                returnValue = typedDatumFactory.createInteger(newValue.longValue());
            }
        }
        
        return returnValue;
    }
    
    /**
     * 
     * This method tries to resolve all numerical Java formats into {@link FloatTD}.
     * 
     * @param value to create the {@link FloatTD} from
     * @param typedDatumFactory 
     * @return Returns null if the value object can not be resolved to {@link FloatTD}.
     */
    private static FloatTD resolveValueToFloatTD(Object value, TypedDatumFactory typedDatumFactory){
        
        FloatTD returnValue = null;
      
        if (value.getClass().equals(Integer.class)) {
            returnValue = typedDatumFactory.createFloat((Integer) value);
        } else if (value.getClass().equals(Long.class)) {
            returnValue = typedDatumFactory.createFloat((Long) value);
        } else if (value.getClass().equals(Double.class)) {
            returnValue = typedDatumFactory.createFloat((Double) value);
        } else if (value.getClass().equals(Float.class)) {
            returnValue = typedDatumFactory.createFloat((Float) value);
        } else if (value.getClass().equals(BigInteger.class)) {
            returnValue = typedDatumFactory.createFloat(((BigInteger) value).doubleValue());
        } else if (value.getClass().equals(String.class)) {
          
            String castValue = (String) value;
            if (castValue.toString().equalsIgnoreCase("+Infinity") || castValue.toString().equalsIgnoreCase("Infinity")) {
                returnValue = typedDatumFactory.createFloat(Double.POSITIVE_INFINITY);
            }
            if (castValue.toString().equalsIgnoreCase("-Infinity")) {
                returnValue = typedDatumFactory.createFloat(Double.NEGATIVE_INFINITY);
            }
            if (castValue.toString().equalsIgnoreCase("NaN")) {
                returnValue = typedDatumFactory.createFloat(Double.NaN);
            }
        } 
        
        return returnValue;
    }
    
    /**
     * 
     * This method tries to resolve numerical and textual Java formats into {@link BooleanTD}.
     * 
     * @param value to create the {@link BooleanTD} from.
     * @param typedDatumFactory 
     * @return Returns always either true or false.
     */
    private static TypedDatum resolveValueToBooleanTD(Object value, TypedDatumFactory typedDatumFactory){
        
        if (value == null) {
            return typedDatumFactory.createBoolean(false);
        }
            
        String stringValue = value.toString();
        TypedDatum returnValue = typedDatumFactory.createBoolean(false);
        boolean isNumber = true;
    
        try {
            double numberValue = Double.parseDouble(stringValue);
            if (Math.abs(numberValue) > 0) {
                returnValue = typedDatumFactory.createBoolean(true);
            } else if (Math.abs(numberValue) < 0) {
                returnValue = typedDatumFactory.createBoolean(false);
            } else {
                isNumber = false;
            }
        } catch (NumberFormatException e) {
            isNumber = false;
        }
    
        if (!isNumber && (stringValue.equalsIgnoreCase("0") || stringValue.equalsIgnoreCase("0L") || stringValue.equalsIgnoreCase("0.0")
            || stringValue.equalsIgnoreCase("0j") || stringValue.equalsIgnoreCase("()") || stringValue.equalsIgnoreCase("[]")
            || stringValue.isEmpty() || stringValue.equalsIgnoreCase("{}") || stringValue.equalsIgnoreCase("false")
            || stringValue.equalsIgnoreCase("none"))) {
    
            returnValue = typedDatumFactory.createBoolean(false);
    
        } else if (!isNumber) {
            returnValue = typedDatumFactory.createBoolean(true);
        }
        
        return returnValue;
    }
    
    private static TypedDatum resolveSimpleValue(Object value, TypedDatumFactory typedDatumFactory){
    
        TypedDatum returnValue = null;
        if (value == null) {
            returnValue = typedDatumFactory.createEmpty();
        } else if (value.getClass().equals(Integer.class)) {
            returnValue = typedDatumFactory.createInteger((Integer) value);
        } else if (value.getClass().equals(Long.class)) {
            returnValue = typedDatumFactory.createInteger((Long) value);
        } else if (value.getClass().equals(BigInteger.class)) {
            BigInteger newValue = (BigInteger) value;
            if (newValue.bitLength() >= LONG_BIT_LENGTH) {
                returnValue = null;
            } else {
                returnValue = typedDatumFactory.createInteger(newValue.longValue());
            }
        } else if (value.getClass().equals(Double.class)) {
            returnValue = typedDatumFactory.createFloat((Double) value);
        } else if (value.getClass().equals(Float.class)) {
            returnValue = typedDatumFactory.createFloat((Float) value);
        } else if (value.getClass().equals(String.class)) {
            
            String castValue = (String) value;
            if (castValue.toString().equalsIgnoreCase("+Infinity") || castValue.toString().equalsIgnoreCase("Infinity")) {
                returnValue = typedDatumFactory.createFloat(Double.POSITIVE_INFINITY);
            } else if (castValue.toString().equalsIgnoreCase("-Infinity")) {
                returnValue = typedDatumFactory.createFloat(Double.NEGATIVE_INFINITY);
            } else if (castValue.toString().equalsIgnoreCase("NaN")) {
                returnValue = typedDatumFactory.createFloat(Double.NaN);
            } else if (castValue.toString().equalsIgnoreCase("true")) {
                returnValue = typedDatumFactory.createBoolean(true);
            } else if (castValue.toString().equalsIgnoreCase("false")) {
                returnValue = typedDatumFactory.createBoolean(false);
            } else {
                returnValue = typedDatumFactory.createShortText(castValue);
            }
            
        } else if (value.getClass().equals(Boolean.class)) {
            returnValue = typedDatumFactory.createBoolean((Boolean) value);
        } else {    
            returnValue = typedDatumFactory.createShortText(value.toString());
        }
        
        if (value instanceof List) {
            return null;
        }
        
        return returnValue;
    }

    private static TypedDatum parseSimpleValue(Object value, TypedDatumFactory typedDatumFactory, DataType requiredType)
        throws ComponentException{
        
        TypedDatum returnValue = null;
        
        if (value == null) {
            returnValue = typedDatumFactory.createEmpty();
        }
        if (requiredType == null) {
            returnValue = resolveSimpleValue(value, typedDatumFactory);
        } else if (requiredType == DataType.Integer) {
            returnValue = resolveValueToIntegerTD(value, typedDatumFactory);
            if (returnValue == null) {
                throw new ComponentException(StringUtils.format("Failed to parse output value '%s' to data type Integer."
                    + " Possible reasons (not restricted): Output value too big (max. 2E"
                    + Long.toBinaryString(Long.MAX_VALUE).length() + " - 1),"
                    + " or output value contains non numeric characters.",
                    value.toString()));
            }
            
        } else if (requiredType == DataType.Float) {
            returnValue = resolveValueToFloatTD(value, typedDatumFactory);
            if (returnValue == null) {
                throw new ComponentException(StringUtils.format("Failed to parse output value '%s' to data type Float."
                    + " Possible reason(not restricted): Output value contains non numeric characters.",
                    value.toString()));
            }
        } else if (requiredType == DataType.Boolean) {
            returnValue = resolveValueToBooleanTD(value, typedDatumFactory);
        } else if (requiredType == DataType.ShortText) {
            returnValue = typedDatumFactory.createShortText(value.toString());
        }
        
        return returnValue;
    }
    
    /**
     * Tries to parse the given Object value into a {@link TypedDatum} that fits the {@link DataType} description.
     * 
     * @param value to create the TypedDatum from
     * @param typedDatumFactory :
     * @param requiredType : The {@link DataType} the value should be parsed to
     * @return Returns null if the parsing did not succeed.
     * @throws ComponentException e
     */
    public static TypedDatum parseToTypedDatum(Object value, TypedDatumFactory typedDatumFactory, DataType requiredType)
        throws ComponentException {
        
        TypedDatum returnValue = null;
                
        if (requiredType == null) {
            
            if (value instanceof List) {
                //TODO Not yet handled, how to differentiate between Matrix or Smalltable
                returnValue = parseListValue(value, typedDatumFactory, null); 
            } else {
                returnValue = parseSimpleValue(value, typedDatumFactory, null); 
            }
            
        } else if (requiredType == DataType.Vector || requiredType == DataType.Matrix || requiredType == DataType.SmallTable) {
            returnValue = parseListValue(value, typedDatumFactory, requiredType);
        } else {
            returnValue = parseSimpleValue(value, typedDatumFactory, requiredType);
        }
        
        return returnValue;
    }
}

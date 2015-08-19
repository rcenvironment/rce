/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.datamodel.types.internal.MatrixTDImpl;
import de.rcenvironment.core.datamodel.types.internal.SmallTableTDImpl;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default {@link TypedDatumConverter} implementation.
 * 
 * @author Robert Mischke
 * @author Jan Flink
 * @author Sascha Zur
 */
class DefaultTypedDatumConverter implements TypedDatumConverter {

    private static final String STRING_CAN_NOT_CONVERT = "Can not convert from %s to %s";

    private static DefaultTypedDatumFactory factory = new DefaultTypedDatumFactory();

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TypedDatum> T castOrConvert(TypedDatum input, Class<T> targetClass) throws DataTypeException {
        return (T) castOrConvert(input, DataType.byTDClass(targetClass));
    }

    @Override
    public TypedDatum castOrConvert(TypedDatum input, DataType targetType) throws DataTypeException {

        DataType inputType = input.getDataType();
        if (inputType.equals(targetType)) {
            return input;
        }
        if (!isConvertibleTo(input, targetType)) {
            throw new DataTypeException(StringUtils.format(STRING_CAN_NOT_CONVERT, input.getDataType().toString(), targetType.toString()));
        }

        if (targetType.equals(DataType.SmallTable)) {
            switch (inputType) {
            case Vector:
            case Matrix:
                // will be converted in second switch statement
                break;
            default:
                // creates a new table with the input object in cell(0,0)
                SmallTableTD smallTable = factory.createSmallTable(1, 1);
                smallTable.setTypedDatumForCell(input, 0, 0);
                return smallTable;
            }
        }

        switch (inputType) {
        case Boolean:
            BooleanTD boolDatum = (BooleanTD) input;
            if (boolDatum.getBooleanValue()) {
                return castOrConvert(factory.createInteger(1), targetType);
            } else {
                return castOrConvert(factory.createInteger(0), targetType);
            }
        case Integer:
            IntegerTD integerDatum = (IntegerTD) input;
            return castOrConvert(factory.createFloat((double) integerDatum.getIntValue()), targetType);
        case Float:
            FloatTD floatDatum = (FloatTD) input;
            VectorTD vector = factory.createVector(1);
            vector.setFloatTDForElement(floatDatum, 0);
            return castOrConvert(vector, targetType);
        case Vector:
            VectorTD vectorDatum = (VectorTD) input;
            FloatTD[][] matrixEntries = new FloatTD[vectorDatum.getRowDimension()][1];
            for (int i = 0; i < vectorDatum.getRowDimension(); i++) {
                matrixEntries[i][0] = vectorDatum.getFloatTDOfElement(i);
            }
            return castOrConvert(new MatrixTDImpl(matrixEntries), targetType);
        case Matrix:
            MatrixTD matrixDatum = (MatrixTD) input;
            return castOrConvert(new SmallTableTDImpl(matrixDatum.toArray()), targetType);
        case DateTime:
            IntegerTD integer = factory.createInteger(((DateTimeTD) input).getDateTimeInMilliseconds());
            return castOrConvert(integer, targetType);
        default:
            throw new DataTypeException(StringUtils.format(STRING_CAN_NOT_CONVERT, inputType.toString(), targetType.toString()));
        }
    }

    @Override
    public boolean isConvertibleTo(TypedDatum input, Class<? extends TypedDatum> targetType) {
        return isConvertibleTo(input, DataType.byTDClass(targetType));
    }

    @Override
    public boolean isConvertibleTo(TypedDatum input, DataType targetType) {
        return TypeDatumConversionTable.getTable()[TypeDatumConversionTable.getIndexOfType(input.getDataType())][TypeDatumConversionTable
            .getIndexOfType(targetType)] == TypeDatumConversionTable.IS_CONVERTIBLE;
    }

    @Override
    public boolean isConvertibleTo(DataType sourceType, DataType targetType) {
        return TypeDatumConversionTable.getTable()[TypeDatumConversionTable.getIndexOfType(sourceType)][TypeDatumConversionTable
            .getIndexOfType(targetType)] == TypeDatumConversionTable.IS_CONVERTIBLE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TypedDatum> T castOrConvertUnsafe(TypedDatum input, Class<T> targetClass) throws DataTypeException {
        return (T) castOrConvertUnsafe(input, DataType.byTDClass(targetClass));
    }

    @Override
    public TypedDatum castOrConvertUnsafe(TypedDatum input, DataType targetType) throws DataTypeException {
        DataType inputType = input.getDataType();
        if (inputType.equals(targetType)) {
            return input;
        }
        if (!isUnsafeConvertibleTo(input, targetType)) {
            throw new DataTypeException(StringUtils.format(STRING_CAN_NOT_CONVERT, inputType.toString(), targetType.toString()));
        }
        // currently, only conversions to ShortText are unsafe possible.
        switch (inputType) {
        case Empty:
            return factory.createShortText("");
        case Boolean:
            return factory.createShortText(input.toString());
        case Integer:
            return factory.createShortText(input.toString());
        case Float:
            return factory.createShortText(input.toString());
        default:
            throw new DataTypeException(StringUtils.format(STRING_CAN_NOT_CONVERT, inputType.toString(), targetType.toString()));
        }
    }

    @Override
    public boolean isUnsafeConvertibleTo(TypedDatum input, Class<? extends TypedDatum> targetType) {
        return isUnsafeConvertibleTo(input, DataType.byTDClass(targetType));

    }

    @Override
    public boolean isUnsafeConvertibleTo(TypedDatum input, DataType targetType) {
        return TypeDatumConversionTable.getTable()[TypeDatumConversionTable.getIndexOfType(input.getDataType())][TypeDatumConversionTable
            .getIndexOfType(targetType)] == TypeDatumConversionTable.IS_UNSAFE_CONVERTIBLE;
    }

    @Override
    public boolean isUnsafeConvertibleTo(DataType sourceType, DataType targetType) {
        return TypeDatumConversionTable.getTable()[TypeDatumConversionTable.getIndexOfType(sourceType)][TypeDatumConversionTable
            .getIndexOfType(targetType)] == TypeDatumConversionTable.IS_UNSAFE_CONVERTIBLE;
    }

}

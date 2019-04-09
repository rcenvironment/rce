/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;

/**
 * Defines and performs conversions between {@link DataType}s.
 * 
 * @author Robert Mischke
 */
public interface TypedDatumConverter {

    /**
     * Transforms the given {@link TypedDatum} into a {@link TypedDatum} of the given target type.
     * The transformation may be performed by modifying properties of the input {@link TypedDatum}
     * or copying its contents to a new object. If no modification is necessary, this method may
     * also return the unmodified input {@link TypedDatum}.
     * 
     * @param input the {@link TypedDatum} to transform
     * @param targetType the desired {@link DataType}, specified by the target Java class
     * @return the new {@link TypedDatum}
     * @throws DataTypeException if the requested conversion is impossible
     * @param <T> equal to targetType; used to specify the return type
     */
    <T extends TypedDatum> T castOrConvert(TypedDatum input, Class<T> targetType) throws DataTypeException;

    /**
     * Transforms the given {@link TypedDatum} into a {@link TypedDatum} of the given target type.
     * The transformation may be performed by modifying properties of the input {@link TypedDatum}
     * or copying its contents to a new object. If no modification is necessary, this method may
     * also return the unmodified input {@link TypedDatum}.
     * 
     * Note that as class information is not available at compile time, this method only returns an
     * unspecified {@link TypedDatum}. If this is unsuitable, use
     * {@link #castOrConvert(TypedDatum, Class)}.
     * 
     * @param input the {@link TypedDatum} to transform
     * @param targetType the desired {@link DataType}
     * @return the new {@link TypedDatum}
     * @throws DataTypeException if the requested conversion is impossible
     */
    TypedDatum castOrConvert(TypedDatum input, DataType targetType) throws DataTypeException;

    /**
     * @param input the source {@link TypedDatum}
     * @param targetType the target type, specified by the target Java class
     * @return true if the source {@link TypedDatum} can be safely converted to the target type
     */
    boolean isConvertibleTo(TypedDatum input, Class<? extends TypedDatum> targetType);

    /**
     * @param input the source {@link TypedDatum}
     * @param targetType the target type
     * @return true if the source {@link TypedDatum} can be safely converted to the target type
     */
    boolean isConvertibleTo(TypedDatum input, DataType targetType);

    /**
     * @param sourceType source {@link DataType}
     * @param targetType the target {@link DataType}
     * @return true if the source {@link TypedDatum} can be safely converted to the target type
     */
    boolean isConvertibleTo(DataType sourceType, DataType targetType);

    /**
     * Transforms the given {@link TypedDatum} into a {@link TypedDatum} of the given target type.<br>
     * <b>This conversion will be unsafe, so a correct conversion the other way is not
     * guaranteed.</b><br>
     * The transformation may be performed by modifying properties of the input {@link TypedDatum}
     * or copying its contents to a new object. If no modification is necessary, this method may
     * also return the unmodified input {@link TypedDatum}.
     * 
     * @param input the {@link TypedDatum} to transform
     * @param targetType the desired {@link DataType}, specified by the target Java class
     * @return the new {@link TypedDatum}
     * @throws DataTypeException if the requested conversion is impossible
     * @param <T> equal to targetType; used to specify the return type
     */
    <T extends TypedDatum> T castOrConvertUnsafe(TypedDatum input, Class<T> targetType) throws DataTypeException;

    /**
     * Transforms the given {@link TypedDatum} into a {@link TypedDatum} of the given target type.<br>
     * <b>This conversion will be unsafe, so a correct conversion the other way is not
     * guaranteed.</b><br>
     * The transformation may be performed by modifying properties of the input {@link TypedDatum}
     * or copying its contents to a new object. If no modification is necessary, this method may
     * also return the unmodified input {@link TypedDatum}.
     * 
     * Note that as class information is not available at compile time, this method only returns an
     * unspecified {@link TypedDatum}. If this is unsuitable, use
     * {@link #castOrConvert(TypedDatum, Class)}.
     * 
     * @param input the {@link TypedDatum} to transform
     * @param targetType the desired {@link DataType}
     * @return the new {@link TypedDatum}
     * @throws DataTypeException if the requested conversion is impossible
     */
    TypedDatum castOrConvertUnsafe(TypedDatum input, DataType targetType) throws DataTypeException;

    /**
     * @param input the source {@link TypedDatum}
     * @param targetType the target type, specified by the target Java class
     * @return true if the source {@link TypedDatum} can be safely converted to the target type
     */
    boolean isUnsafeConvertibleTo(TypedDatum input, Class<? extends TypedDatum> targetType);

    /**
     * @param input the source {@link TypedDatum}
     * @param targetType the target type
     * @return true if the source {@link TypedDatum} can be safely converted to the target type
     */
    boolean isUnsafeConvertibleTo(TypedDatum input, DataType targetType);

    /**
     * @param sourceType source {@link DataType}
     * @param targetType the target {@link DataType}
     * @return true if the source {@link TypedDatum} can be safely converted to the target type
     */
    boolean isUnsafeConvertibleTo(DataType sourceType, DataType targetType);
}

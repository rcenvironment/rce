/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.channel.legacy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;
import de.rcenvironment.core.utils.common.variables.legacy.VariableType;


/**
 * This class represents an n-dimensional array where each cell may contain its own data value and type.
 *
 * @author Arne Bachmann
 */
@Deprecated
public class VariantArray implements Serializable {
    
    private static final String EXCEPTION_WRONGDIMENSIONS = "Wrong number of dimensions provided in VariantArray";

    /**
     * For serialization.
     */
    private static final long serialVersionUID = -974534708839659363L;
    
    /**
     * Name of the array.
     */
    private final String name;

    /**
     * Sizes of dimensions.
     */
    private final int[] dimensions;
    
    /**
     * Dimension multipliers for index determination.
     */
    private final int[] multipliers;
    
    /**
     * All dimensions in one array.
     */
    private TypedValue[] array;
    
    /**
     * Marker for compressed state.
     */
    private boolean compressed = false;
    
    
    /**
     * Constructor where given values are delivered.
     * 
     * @param theName name of VariantArray
     * @param values values which are delivered
     * @param dimensionSizes Array of dimensions or variable number of arguments for dimension size (e.g. 3,4,5 = 3x4x5 3D-matrix).
     */
    private VariantArray(final String theName, final TypedValue[] values, final int... dimensionSizes) {
        if ((dimensionSizes == null) || (dimensionSizes.length <= 0)) {
            throw new IllegalArgumentException(EXCEPTION_WRONGDIMENSIONS);
        }
        name = theName;
        dimensions = dimensionSizes;
        multipliers = calculateMultipliers(dimensions);
        array = values;
    }
    
    /**
     * The default constructor.
     * 
     * @param dimensions Array of dimensions or variable number of arguments for dimension size (e.g. 3,4,5 = 3x4x5 3D-matrix).
     */
    public VariantArray(final String theName, final int... dimensionSizes) {
        if ((dimensionSizes == null) || (dimensionSizes.length <= 0)) {
            throw new IllegalArgumentException(EXCEPTION_WRONGDIMENSIONS);
        }
        name = theName;
        dimensions = dimensionSizes;
        multipliers = calculateMultipliers(dimensions);
        array = new TypedValue[getSize()];
        setDefaultType(VariableType.Empty); // initialize all cells
    }
    
    /**
     * Copy constructor.
     * 
     * @param from The object to copy values from
     */
    public VariantArray(final String theName, final VariantArray from) {
        if (from == null) {
            throw new IllegalArgumentException("Cannot use copy constructor with null argument");
        }
        name = theName;
        dimensions = new int[from.dimensions.length];
        multipliers = new int[from.multipliers.length];
        array = new TypedValue[from.array.length];
        System.arraycopy(from.dimensions, 0, dimensions, 0, from.dimensions.length);
        System.arraycopy(from.multipliers, 0, multipliers, 0, from.multipliers.length);
        System.arraycopy(from.array, 0, array, 0, from.array.length);
    }
 
    
    /**
     * Set a cell value.
     * 
     * @param value The value to set
     * @param index The n dimension indexes, starting with 0
     * @return this
     */
    public VariantArray setValue(final TypedValue value, final int... index) {
        array[index(index)] = new TypedValue(value);
        return this;
    }
    
    /**
     * Add VariantArray to existing VariantArray. 
     * Source array needs the "root" dimension which will be extended with the parameter VariantArray.
     * Example:
     * If arrayToAdd VariantArray va = [3,4,5]
     * and main VariantArray = [1,3,4,5]
     * then returning VariantArray = [2,3,4,5].
     *
     * @param main VariantArray
     * @param arrayToAdd Variant Array parameter
     * @return this Variant Array as copy
     */
    public static VariantArray addValuesToVariantArray(final VariantArray main, final VariantArray arrayToAdd) {
        if (main.dimensions.length - 1 != arrayToAdd.dimensions.length) {
            throw new IllegalArgumentException(EXCEPTION_WRONGDIMENSIONS);
        }
        int counter = 0;
        for (int i: arrayToAdd.dimensions) {
            if (main.dimensions[counter + 1] != i) {
                throw new IllegalArgumentException("Wrong structure of dimensions provided in VariantArray");
            }
            counter++;
        }
        
        TypedValue[] newArray = new TypedValue[main.array.length + arrayToAdd.getSize()];
        System.arraycopy(main.array, 0, newArray, 0, main.array.length);
        System.arraycopy(arrayToAdd.array, 0, newArray, main.array.length, arrayToAdd.array.length);
        
        int[] newDimensions = main.dimensions;
        newDimensions[0] = newDimensions[0] + 1;
                
        return new VariantArray(main.name, newArray, newDimensions);
    }
    
    /**
     * Set a cell value.
     * 
     * @param value The value to set
     * @param index The n dimension indexes, starting with 0
     * @return this
     * throws an illegal argument exception if type differs from previous type
     */
    public VariantArray setValue(final Serializable value, final int... index) {
        array[index(index)] = new TypedValue(value);
        return this;
    }
    
    /**
     * Get a cell value.
     * 
     * @param index The n dimension indexes, starting with 0.
     * @return The value
     */
    public TypedValue getValue(final int... index) {
        return array[index(index)];
    }
    
    /**
     * Initialize basic type and value for all cells, to avoid illegal values when forgetting to set singular cells.
     * 
     * @param value The value to set, of arbitrary type.
     * @return this
     */
    public VariantArray setDefaultValue(final Serializable value) {
        for (int i = 0; i < array.length; i ++) {
            array[i] = new TypedValue(value);
        }
        return this;
    }
    
    /**
     * Define the default type for all cells, to avoid illegal values when forgetting to set singular cells.
     * 
     * @param type The type to assume for each cell by default
     * @return this
     */
    public VariantArray setDefaultType(final VariableType type) {
        if (type == VariableType.Empty) {
            for (int i = 0; i < array.length; i ++) {
                array[i] = TypedValue.EMPTY;
            }
        } else {
            for (int i = 0; i < array.length; i ++) {
                array[i] = new TypedValue(type);
            }
        }
        return this;
    }
    
    /**
     * Return the array's name.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Convenience getter.
     * 
     * @return The dimensions
     */
    public int[] getDimensions() {
        return dimensions;
    }

    /**
     * Return the number of elements in the array.
     * 
     * @return The number of elements in the array
     */
    public int getSize() {
        if (compressed) {
            return array.length;
        }
        return multipliers[0]; // all dimension sizes multiplied = number of cells
    }

    /**
     * Return true if all values have been set and there is no empty value (set or unset) in the array.
     * 
     * @return True if all values have been set
     */
    public boolean isMatrixComplete() {
        for (final TypedValue value: array) {
            if ((value == null) || (value == TypedValue.EMPTY) || (value.getType() == VariableType.Empty)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculate index position of flat index.
     * 
     * @param index The flat index of the array
     * @return The dimensioned index from least- to most-significant
     */
    public int[] getIndex(int index) {
        final int max = dimensions.length - 1;
        final int[] position = new int[max + 1];
        position[max] = index % dimensions[max];
        for (int i = max - 1; i >= 0; i --) {
            position[i] = (index % multipliers[i]) / multipliers[i + 1];
        }
        return position;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VariantArray ");
        if (name != null) {
            sb.append(name);
        }
        sb.append("[");
        for (int d = 0; d < dimensions.length - 1; d ++) {
            sb.append(Integer.toString(dimensions[d]));
            sb.append(",");
        }
        sb.append(Integer.toString(dimensions[dimensions.length - 1]));
        sb.append("]");
        return sb.toString();
    }

    /**
     * Return dimension multipliers.
     * 
     * @param dimensions All dimension sizes
     * @return The dimension multipliers/dividers
     */
    protected static int[] calculateMultipliers(final int[] dimensions) {
        final int[] ret = new int[dimensions.length];
        int product = 1;
        for (int d = dimensions.length - 1; d >= 0; d --) {
            product *= dimensions[d];
            ret[d] = product;
        }
        return ret;
    }
    
    /**
     * Get the index of an element.
     * 
     * @param indexes The index of each dimensions
     * @return The index, 0-based
     */
    protected int index(final int[] indexes) {
        int ret = 0;
        for (int i = 0; i < indexes.length - 1; i ++) {
            ret += multipliers[i + 1] * indexes[i];
        }
        ret += indexes[indexes.length - 1]; // dimension 0 has always multiplier 1
        return ret;
    }
    
    /**
     * Remove all empty hyper-slices of the given dimension.
     * This means fixing the dimension and checking all other values for emptyness.
     * If empty, remove the fixed index and decrease it (increase it if ascending).
     * If everything is pruned, an n-dimensional array with exactly one empty cell is returned.
     * 
     * @param dimension The fixed dimensions index (0 = highest, n-1 = lowest)
     * @param lowest True if checking 0..n-1 ("right-to-left")
     * @param highest True if checking n-1..0 ("left-to-right")
     * @return A new pruned array
     */
    public VariantArray pruneDimension(final int dimension, final boolean lowest, final boolean highest) {
        if (!lowest && !highest) {
            return this; // nothing to do
        }
        int count;
        int toPruneLowest = 0; // found hyper-slices to remove
        int toPruneHighest = 0; // found hyper-slices to remove
        int hypersliceElements = multipliers[0] / dimensions[dimension];
        
        if (lowest) { // lowest to highest pruning
            for (int i = 0; i < dimensions[dimension]; i ++) { // starting from lowest index value in fixed dimension
                count = 0; // counter of empty cell in the hyperslice
                for (int cell = 0; cell < multipliers[0]; cell ++) { // for all cells
                    final int[] tmp = getIndex(cell);
                    if ((array[cell].getType() == VariableType.Empty) && (tmp[dimension] == i)) { // ignore fixed 
                        count ++;
                    }
                }
                if (count == hypersliceElements) { // hyper-slice is completely empty: mark for pruning
                    toPruneLowest ++;
                    continue;
                } else {
                    break;
                }
            }
        }
        
        if (highest && (toPruneLowest < dimensions[dimension])) { // only check ohif we don't already know that everything is empty
            for (int i = dimensions[dimension] - 1; i >= 0; i --) {
                count = 0; // counter of empty cells
                for (int cell = 0; cell < multipliers[0]; cell ++) { // for all cells
                    final int[] tmp = getIndex(cell);
                    if ((array[cell].getType() == VariableType.Empty) && (tmp[dimension] == i)) { // only count our fixed hyperslice 
                        count ++;
                    }
                }
                if (count == hypersliceElements) { // hyper-slice is completely empty: mark for pruning
                    toPruneHighest ++;
                    continue;
                } else {
                    break;
                }
            }
        }

        // prepare new array
        if ((toPruneLowest + toPruneHighest) >= dimensions[dimension]) { // nothing left after pruning
            toPruneLowest = 0;
            toPruneHighest = dimensions[dimension];
        }
        int reduce = toPruneLowest + toPruneHighest;
        if (reduce == 0) {
            return new VariantArray(name, this);
        }
        final int[] newDimensions = new int[dimensions.length];
        System.arraycopy(dimensions, 0, newDimensions, 0, dimensions.length);
        newDimensions[dimension] -= reduce; // reduce dimensional size
        toPruneHighest = dimensions[dimension] - toPruneHighest;
        final VariantArray newArray = new VariantArray(name, newDimensions);
        
        int newCell = 0;
        for (int cell = 0; cell < multipliers[0]; cell ++) { // for every cell in the old array
            final int[] tmp = getIndex(cell);
            final int i = tmp[dimension];
            if ((i >= toPruneLowest) && (i < toPruneHighest)) {
                newArray.array[newCell ++] = array[cell]; // only copy non-pruned entries
            }
        }
        return newArray;
    }
    
    /**
     * Prune highest dimension if empty.
     * 
     * @return VariantArray with no empty entries on lowest dimension.
     */
    public VariantArray pruneDimensionZero() {
        VariantArray va;
        List<VariantArray> list = unitizeHighestDimension();

        for (int i = (list.size() - 1); i >= 0; i--) {
            VariantArray entry = list.get(i);
            
            if (entry.isEmpty()) {
                list.remove(entry);
            } else {
                break;
            }
        }
        
        
        // Rebuild VariantArray      
        // new dimension description
        int[] finalDimensions = new int[1 + list.get(0).getDimensions().length];
        System.arraycopy(list.get(0).getDimensions(), 0, finalDimensions, 1, finalDimensions.length - 1);
        va = new VariantArray(list.get(0).getName(), finalDimensions);
        
        // Copy values to...
        for (VariantArray slice: list) {
            va = VariantArray.addValuesToVariantArray(va, slice);
        }
        
        
        return va;
    }
    
    /**
     * Checks if content is empty.
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        boolean isEmptyContent = false;
        if (array.length == 0) {
            isEmptyContent = true;
        }
        
        for (TypedValue val: array) {
            if (val.getStringValue() == null || val.getStringValue().equals("") || val.getType() == VariableType.Empty) {
                isEmptyContent = true;
            } else {
                isEmptyContent = false;
                break;
            }
        }
        
        return isEmptyContent;
    }
    
    /**
     * Compresses the memory usage.
     * Needs to uncompress before further usage.
     * @return self
     */
    public VariantArray compress() {
        final List<TypedValue> list = new ArrayList<TypedValue>();
        TypedValue last = null;
        int count = 0;
        for (final TypedValue tv: array) {
            if (last == null) {
                last = tv;
                count = 1;
                continue;
            }
            if (last.getType() == VariableType.Empty) {
                if (tv.getType() == VariableType.Empty) {
                    count ++;
                } else {
                    writeOutRle(count, last, list);
                    count = 1;
                    last = tv;
                }
            } else { // last was not empty
                if (tv.getType() == VariableType.Empty) {
                    writeOutRle(count, last, list);
                    count = 1;
                    last = tv;
                } else { // need to compare
                    if ((tv.getType() == last.getType()) && (tv.getValue().equals(last.getValue()))) {
                        count ++;
                    } else {
                        writeOutRle(count, last, list);
                        count = 1;
                        last = tv;
                    }
                }
            }
        }
        writeOutRle(count, /* end */ last, list);
        compressed = true;
        array = list.toArray(new TypedValue[0]); // replace data
        return this;
    }

    /**
     * Repetitive work.
     * 
     * @param count The count to store
     * @param value The value to write
     * @param list The list to add to
     */
    private static void writeOutRle(final int count, final TypedValue value, final List<TypedValue> list) {
        if (count > 1) {
            list.add(new TypedValue(VariableType.RLE, Integer.toString(count)));
        }
        list.add(value);
    }
    
    /**
     * Uncompressed the memory usage.
     * Necessary to use other methods.
     * @return self
     */
    public VariantArray uncompress() {
        if (!compressed) {
            return this;
        }
        final TypedValue[] old = array;
        array = new TypedValue[multipliers[0]];
        int source = 0;
        int target = 0;
        while (source < old.length) {
            final TypedValue tv = old[source ++];
            if (tv.getType() == VariableType.RLE) {
                final TypedValue rleValue = old[source ++];
                final int count = Integer.valueOf((String) tv.getValue());
                for (int c = 0; c < count; c ++) {
                    if (rleValue.getType() == VariableType.Empty) {
                        array[target ++] = TypedValue.EMPTY;
                    } else {
                        array[target ++] = new TypedValue(rleValue); // copy constructor
                    }
                }
            } else {
                array[target ++] = tv;
            }
        }
        compressed = false;
        return this;
    }
    
    /**
     * For test classes.
     * 
     * @return The array
     */
    protected TypedValue[] getInternalArray() {
        return array;
    }
    
    /**
     * For external checking.
     * 
     * @return true if compressed
     */
    public boolean isCompressed() {
        return compressed;
    } 
    
    /**
     * Unitize highest dimension of a VariantArray. 
     * Split every set of highest dimension array into a piece.
     * 
     * @return list of highest dimension pieces
     */
    public List<VariantArray> unitizeHighestDimension() {
        List<VariantArray> list = new ArrayList<VariantArray>();
        
        if (dimensions.length == 1) {
            // On 1-dimension
            int sizeDimension = dimensions[0];
            for (int dimensionrunner = 0; dimensionrunner < sizeDimension; dimensionrunner++) {
                TypedValue[] newValueArray = new TypedValue[1]; 
                newValueArray[0] = getValue(dimensionrunner);
                VariantArray va = new VariantArray(name, newValueArray, 1);
                list.add(va);
            }
            
        } else {
            int sizeOfHighestDimension = dimensions[0];
            int pointerToEmptyEntry = 0;
            for (int globalrunner = 1; globalrunner <= sizeOfHighestDimension; globalrunner++) {
                TypedValue[] newValueArray = new TypedValue[getSize() / sizeOfHighestDimension];

                for (int index = 0; index < newValueArray.length; index++) {
                    newValueArray[index] = getValue(getIndex(pointerToEmptyEntry));
                    pointerToEmptyEntry++;
                }

                //Create new VariantArray
                int[] d = cutFirstEntry(dimensions);
                VariantArray va = new VariantArray(name, newValueArray, d);
                list.add(va);
            }
        }
        return list;
    }
    
    
   
    /**
     * Cut first entry from array.
     * 
     * @param array array to cut
     * @return new array without first entry
     */
    private static int[] cutFirstEntry(int[] array) {
        int[] result = new int[array.length - 1];
        
        for (int i = 0; i < result.length; i++) {
            result[i] = array[i + 1];
        }
        
        /*
         * Special handling for 1-dimensional arrays.
         * 
         * 1-dimension should not be described as [2], but as [2, 1].
         */
        if (result.length == 1) {
            int value = result[0];
            result = new int[2];
            result[1] = 1;
            result[0] = value;
        }
        
        return result;
    }

}

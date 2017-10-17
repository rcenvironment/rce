/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Constant class for holding the conversion table for all {@link TypedDatum}. Underscores (_) are
 * for satisfying the auto formatting rules while keeping a nice look.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public final class TypedDatumConversionTable {

    /** Constant if a conversion is ok. */
    public static final int IS_CONVERTIBLE = 0;

    /** Constant if a conversion is forbidden. */
    public static final int IS_NOT_CONVERTIBLE = 1;

    /**
     * Constant if a conversion is ok but unsafe.
     * @deprecated unsafe conversion is under review and is likely to be removed in 8.0 (see https://mantis.sc.dlr.de/view.php?id=13787)
     **/
    @Deprecated
    public static final int IS_UNSAFE_CONVERTIBLE = 2;

    private static final int NO_ = IS_NOT_CONVERTIBLE;

    private static final int YES = IS_CONVERTIBLE;

    private static final int UNS = IS_UNSAFE_CONVERTIBLE;

    /**
     * table with the conversion values.
     */
    private static final int[][] TABLE =
    {

        /* FROM_\/______TO> Epty Bool Int_ Flt_ Vctr Mtrx SmlT STxt FilR Date BigT DirR SDat */
        /* Empty_______ */{ NO_, NO_, NO_, NO_, NO_, NO_, YES, UNS, NO_, NO_, NO_, NO_, NO_ },
        /* Boolean_____ */{ NO_, NO_, YES, YES, YES, YES, YES, UNS, NO_, NO_, NO_, NO_, NO_ },
        /* Integer_____ */{ NO_, NO_, NO_, YES, YES, YES, YES, UNS, NO_, NO_, NO_, NO_, NO_ },
        /* Float_______ */{ NO_, NO_, NO_, NO_, YES, YES, YES, UNS, NO_, NO_, NO_, NO_, NO_ },
        /* Vector______ */{ NO_, NO_, NO_, NO_, NO_, YES, YES, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* Matrix______ */{ NO_, NO_, NO_, NO_, NO_, NO_, YES, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* SmallTable__ */{ NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* ShortText___ */{ NO_, NO_, NO_, NO_, NO_, NO_, YES, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* FileRef_____ */{ NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* DateTime____ */{ NO_, NO_, YES, YES, YES, YES, YES, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* BigTable____ */{ NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* DirectoryRef */{ NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_ },
        /* StructData__ */{ NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_, NO_ }
    };

    private static final DataType[] TYPE_INDICES = {
        DataType.Empty, DataType.Boolean, DataType.Integer, DataType.Float, DataType.Vector, DataType.Matrix, DataType.SmallTable,
        DataType.ShortText, DataType.FileReference,
        DataType.DateTime, DataType.BigTable, DataType.DirectoryReference, DataType.StructuredData
    };

    private TypedDatumConversionTable() {}

    /**
     * Returns the index of the given type in the conversion table.
     * 
     * @param type to search for
     * @return index in the conversion table
     */
    public static int getIndexOfType(DataType type) {
        int index = 0;
        for (int i = 0; i < TYPE_INDICES.length; i++) {
            if (type == TYPE_INDICES[i]) {
                index = i;
            }
        }
        return index;
    }

    public static int[][] getTable() {
        return TABLE;
    }

}

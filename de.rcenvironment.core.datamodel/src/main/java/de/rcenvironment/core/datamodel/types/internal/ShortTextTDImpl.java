/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.types.internal;

import org.apache.commons.lang3.StringUtils;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;

/**
 * Implementation of {@link ShortTextTD}.

 * @author Doreen Seider
 */
public class ShortTextTDImpl extends AbstractTypedDatum implements ShortTextTD {

    /** Maximum character. */
    public static final int MAXIMUM_LENGTH = 140;
    
    private final String shortText;
    
    public ShortTextTDImpl(String shortText) {
        super(DataType.ShortText);
        if (shortText.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("text exceeds maximum character of 140. it has: " + shortText.length());
        }
        this.shortText = shortText;
    }

    @Override
    public String getShortTextValue() {
        return shortText;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof ShortTextTD) {
            ShortTextTD other = (ShortTextTD) obj;
            return shortText.equals(other.getShortTextValue());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return shortText.hashCode();
    }
    
    @Override
    public String toString() {
        return getShortTextValue();
    }

    @Override
    public String toLengthLimitedString(int maxLength) {
        return StringUtils.abbreviate(getShortTextValue(), maxLength);
    }

}

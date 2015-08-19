/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.types.internal;

import java.util.Date;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;

/**
 * Implementation of {@link DateTimeTD}.
 *
 * @author Doreen Seider
 */
public class DateTimeTDImpl extends AbstractTypedDatum implements DateTimeTD {

    private final long dateTime;
    
    public DateTimeTDImpl(long dateTime) {
        super(DataType.DateTime);
        this.dateTime = dateTime;
    }

    @Override
    public Date getDateTime() {
        return new Date(dateTime);
    }
    
    @Override
    public long getDateTimeInMilliseconds() {
        return dateTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int randomDigit = 32;
        int result = 1;
        result = prime * result + (int) (dateTime ^ (dateTime >>> randomDigit));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof DateTimeTD) {
            DateTimeTD other = (DateTimeTD) obj;
            return dateTime == other.getDateTimeInMilliseconds();
        }
        return true;
    }
    
    

}

/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.EmptyTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import junit.framework.Assert;

/**
 * Test class for the {@link ScriptDataTypeHelper}.
 * 
 * @author Sascha Zur
 */
public class ScriptDataTypeHelperTest {

    /**
     * Test method getObjectOfEntryForPythonOrJython with several data types.
     */
    @Test
    public void testGetObjectOfEntryForPythonOrJython() {
        Object nullObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(null);
        Assert.assertEquals("None", nullObj);

        EmptyTD empty = EasyMock.createStrictMock(EmptyTD.class);
        EasyMock.expect(empty.getDataType()).andReturn(DataType.Empty).once();
        EasyMock.replay(empty);
        Object emptyObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(empty);
        Assert.assertEquals("None", emptyObj);

        BooleanTD booleanTDTrue = EasyMock.createStrictMock(BooleanTD.class);
        EasyMock.expect(booleanTDTrue.getDataType()).andReturn(DataType.Boolean).anyTimes();
        EasyMock.expect(booleanTDTrue.getBooleanValue()).andReturn(true).anyTimes();
        EasyMock.replay(booleanTDTrue);
        Object boolObjTrue = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(booleanTDTrue);
        Assert.assertEquals("True", boolObjTrue);

        BooleanTD booleanTDFalse = EasyMock.createStrictMock(BooleanTD.class);
        EasyMock.expect(booleanTDFalse.getDataType()).andReturn(DataType.Boolean).anyTimes();
        EasyMock.expect(booleanTDFalse.getBooleanValue()).andReturn(false).anyTimes();
        EasyMock.replay(booleanTDFalse);
        Object boolObjFalse = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(booleanTDFalse);
        Assert.assertEquals("False", boolObjFalse);

        FloatTD floatTD = EasyMock.createStrictMock(FloatTD.class);
        EasyMock.expect(floatTD.getDataType()).andReturn(DataType.Float).anyTimes();
        EasyMock.expect(floatTD.getFloatValue()).andReturn(1.0).anyTimes();
        EasyMock.replay(floatTD);
        Object floatObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(floatTD);
        Assert.assertEquals(1.0, floatObj);

        IntegerTD integerTD = EasyMock.createStrictMock(IntegerTD.class);
        EasyMock.expect(integerTD.getDataType()).andReturn(DataType.Integer).anyTimes();
        EasyMock.expect(integerTD.getIntValue()).andReturn(5L).anyTimes();
        EasyMock.replay(integerTD);
        Object intObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(integerTD);
        Assert.assertEquals(5L, intObj);

        ShortTextTD shorttextTD = EasyMock.createStrictMock(ShortTextTD.class);
        EasyMock.expect(shorttextTD.getDataType()).andReturn(DataType.ShortText).anyTimes();
        EasyMock.expect(shorttextTD.getShortTextValue()).andReturn("This is a test").anyTimes();
        EasyMock.replay(shorttextTD);
        Object shortObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(shorttextTD);
        Assert.assertEquals("This is a test", shortObj);

    }
}

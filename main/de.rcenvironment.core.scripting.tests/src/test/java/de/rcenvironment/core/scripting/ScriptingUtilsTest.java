/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.scripting;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.testutils.TypedDatumServiceDefaultStub;

/**
 *
 * Basic unit tests for the {@link ScriptingUtils}.
 *
 * @author David Scholz
 */
public final class ScriptingUtilsTest {

    /**
     * 
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ScriptingUtils scriptingUtils = new ScriptingUtils();

    public ScriptingUtilsTest() {
        scriptingUtils.bindTypedDatumService(new TypedDatumServiceDefaultStub());
    }

    /**
     * 
     */
    @Before
    public void setUp() {

    }

    /**
     * Tests behavior of
     * {@link ScriptingUtils#writeOutputByType(Object, de.rcenvironment.core.datamodel.api.DataType, String, 
     * String, javax.script.ScriptEngine, de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem, 
     * de.rcenvironment.core.component.execution.api.ComponentContext)}
     * for writing small tables with different row element count.
     * 
     * @throws ComponentException on failure.
     */
    @Test
    public void testInvalidRowElementCountForSmallTable() throws ComponentException {
        exception.expect(ComponentException.class);
        exception.expectMessage("Each row must have the same number of elements in a small table.");

        List<List<Object>> smallTableList = new ArrayList<>();
        List<Object> firstRow = new ArrayList<>();
        List<Object> secRow = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            firstRow.add(i);
            if (i == 0) {
                continue;
            }
            secRow.add(i);
        }
        smallTableList.add(firstRow);
        smallTableList.add(secRow);

        ScriptingUtils.writeOutputByType(smallTableList, DataType.SmallTable, "hellö", null, null, null, null);
    }

}

/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.channel.legacy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;
import de.rcenvironment.core.utils.common.variables.legacy.VariableType;

/**
 * Test the array functions.
 * 
 * @author Arne Bachmann
 */
@SuppressWarnings("deprecation") // Keep test for deprecated class VariantArray
public class VariantArrayTest {

    private static final long A_INTEGER = 20L;

    private static final double A_REAL = 30D;

    /**
     * Check exception throwing.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongDimensions1() {
        new VariantArray(null, (VariantArray) null);
    }

    /**
     * Check exception throwing.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongDimensions2() {
        new VariantArray(null, new int[0]);
    }

    /**
     * There are some static functions.
     */
    @Test
    public void testStaticFunctions() {
        assertThat(VariantArray.calculateMultipliers(new int[] { 2, 3 }), is(new int[] { 6, 3 }));
    }

    /**
     * Check all functions.
     */
    @Test
    public void testFunctions() {
        final VariantArray a = new VariantArray("myarray", 2, 3); // 2x3 matrix
        assertThat(a.getName(), is("myarray"));
        assertThat(a.getDimensions(), is(new int[] { 2, 3 }));
        assertThat(a.getSize(), is(2 * 3));
        assertThat(a.getIndex(0), is(new int[] { 0, 0 })); // reversed order (watch it!)
        assertThat(a.getIndex(3), is(new int[] { 1, 0 }));
        assertThat(a.getIndex(5), is(new int[] { 1, 2 }));
        a.setValue("bla", new int[] { 0, 1 });
        a.setValue(new TypedValue(true), 0, 2);
        a.setValue(A_REAL, 1, 1);
        a.setValue(A_INTEGER, 1, 2);
        assertFalse(a.isMatrixComplete()); // we left out two cells
        assertThat(a.getValue(0, 0), is(TypedValue.EMPTY)); // the value was never initialized
        assertThat(a.getValue(0, 1).getType(), is(VariableType.String));
        assertThat(a.getValue(0, 2).getType(), is(VariableType.Logic));
        assertThat(a.getValue(1, 1).getType(), is(VariableType.Real));
        assertThat(a.getValue(1, 2).getType(), is(VariableType.Integer));
        assertThat(a.getValue(0, 1).getStringValue(), is("bla"));
        assertThat(a.getValue(0, 2).getLogicValue(), is(true));
        assertThat(a.getValue(1, 1).getRealValue(), is(A_REAL));
        assertThat(a.getValue(1, 2).getIntegerValue(), is(A_INTEGER));

        final VariantArray b = new VariantArray("myarray2", a);
        assertThat(b.getDimensions(), is(new int[] { 2, 3 }));
        assertThat(b.getName(), is("myarray2"));
        b.setValue(2, 0, 0); // slot was missing
        b.setValue("true", 1, 0); // slot was missing
        assertTrue(b.isMatrixComplete());
        assertThat(b.getValue(1, 2).getIntegerValue(), is(A_INTEGER));
        assertThat(b.index(new int[] { 1, 2 }), is(5));

        final VariantArray c = new VariantArray("myarray3", new int[] { 40, 40, 3 });
        c.setDefaultType(VariableType.Integer);
        assertTrue(c.isMatrixComplete());
        assertThat(c.getValue(10, 10, 1).getType(), is(VariableType.Integer));

        final VariantArray d = new VariantArray("myarray3", new int[] { 40, 40, 3 });
        d.setDefaultValue(A_REAL);
        assertTrue(d.isMatrixComplete());
        assertThat(d.getValue(10, 10, 1).getType(), is(VariableType.Real));
        assertThat(d.getValue(10, 10, 1).getRealValue(), is(A_REAL));
    }

    /**
     * Since the variantarray worked well for 2D, but not for 3D, we introduced these test cases.
     */
    @Test
    public void test3D() {
        final int x20 = 20;
        final int x21 = 21;
        final int x59 = 59;
        final VariantArray a = new VariantArray("z", 3, 4, 5);
        assertThat(a.getIndex(0), is(new int[] { 0, 0, 0 }));
        assertThat(a.getIndex(1), is(new int[] { 0, 0, 1 }));
        assertThat(a.getIndex(5), is(new int[] { 0, 1, 0 }));
        assertThat(a.getIndex(6), is(new int[] { 0, 1, 1 }));
        assertThat(a.getIndex(10), is(new int[] { 0, 2, 0 }));
        assertThat(a.getIndex(x20), is(new int[] { 1, 0, 0 }));
        assertThat(a.getIndex(x21), is(new int[] { 1, 0, 1 }));
        assertThat(a.getIndex(x59), is(new int[] { 2, 3, 4 }));
    }

    /**
     * Test pruning of empty hyper-slices in array.
     */
    @Test
    public void testPruning() {
        final VariantArray a = new VariantArray("test", 4, 4);
        a.setValue((Serializable) null, new int[] { 0, 0 });
        a.setValue((Serializable) null, new int[] { 0, 1 });
        a.setValue((Serializable) null, new int[] { 0, 2 });
        a.setValue((Serializable) null, new int[] { 0, 3 });

        a.setValue((Serializable) null, new int[] { 1, 0 });
        a.setValue("x", new int[] { 1, 1 });
        a.setValue(1.0D, new int[] { 1, 2 });
        a.setValue((Serializable) null, new int[] { 1, 3 });

        a.setValue((Serializable) null, new int[] { 2, 0 });
        a.setValue(9, new int[] { 2, 1 });
        a.setValue(Boolean.TRUE, new int[] { 2, 2 });
        a.setValue((Serializable) null, new int[] { 2, 3 });

        a.setValue((Serializable) null, new int[] { 3, 0 });
        a.setValue((Serializable) null, new int[] { 3, 1 });
        a.setValue((Serializable) null, new int[] { 3, 2 });
        a.setValue((Serializable) null, new int[] { 3, 3 });

        // highest dimension from bottom ("vertical" upwards in matrix)
        final VariantArray b = a.pruneDimension(0, false, true);
        assertThat(b.getDimensions(), is(new int[] { 3, 4 }));
        assertThat(b.getValue(0, 2).getType(), is(VariableType.Empty));
        assertThat(b.getValue(0, 3).getValue(), is(b.getValue(1, 0).getValue())); // check wrong
                                                                                  // index

        // highest dimension from top ("vertical" downwards in matrix)
        final VariantArray c = b.pruneDimension(0, true, false);
        assertThat(c.getDimensions(), is(new int[] { 2, 4 }));
        assertThat(c.getValue(0, 1).getType(), is(VariableType.String));
        assertThat((String) c.getValue(0, 1).getValue(), is("x"));

        // lowest dimension from left ("horizontal" rightwards in matrix)
        final VariantArray d = c.pruneDimension(1, true, false);
        assertThat(d.getDimensions(), is(new int[] { 2, 3 }));
        assertThat(d.getValue(1, 0).getType(), is(VariableType.Integer));
        assertThat((Long) d.getValue(1, 0).getValue(), is(9L));

        // lowest dimension from top ("horizontal" leftwards in matrix)
        final VariantArray e = d.pruneDimension(1, false, true);
        assertThat(e.getDimensions(), is(new int[] { 2, 2 }));
        assertThat(e.getValue(0, 1).getType(), is(VariableType.Real));
        assertThat((Double) e.getValue(0, 1).getValue(), is(1.0D));
        assertTrue(e.isMatrixComplete());

        // highest dimension from both sides at the same time
        final VariantArray f = a.pruneDimension(0, true, true);
        assertThat(f.getDimensions(), is(new int[] { 2, 4 }));

        // lowest dimension from both sides at the same time
        final VariantArray g = f.pruneDimension(1, true, true);
        assertThat(g.getDimensions(), is(new int[] { 2, 2 }));
    }

    /**
     * Test duration and if memory explodes and if large array is pruned correctly.
     */
    @Test
    public void testLargeArray() {
        final int x1000 = 1000;
        final int x20 = 20;
        final int x100000 = 100000;

        final int allowedTestDuration = 2000; // 2 sec

        final long msStart = System.currentTimeMillis();
        final VariantArray a = new VariantArray("abc", new int[] { x1000, 20, 5 }); // 1e6 cells
        for (int i = 10; i < x1000 - 10; i++) {
            for (int j = 2; j < x20 - 2; j++) {
                for (int k = 1; k < 5 - 1; k++) {
                    a.setValue(Math.PI, i, j, k);
                }
            }
        }
        assertThat(a.getSize(), is(x100000));
        final VariantArray b = a.pruneDimension(0, true, true);
        assertThat(b.getDimensions(), is(new int[] { x1000 - x20, x20, 5 }));
        final VariantArray c = b.pruneDimension(1, true, true);
        final VariantArray d = c.pruneDimension(2, true, true);
        assertThat(d.getDimensions(), is(new int[] { x1000 - x20, x20 - 4, 5 - 2 }));
        assertTrue(d.isMatrixComplete());
        final long msDuration = System.currentTimeMillis() - msStart;

        // check for reasonable speed
        assertTrue("Test duration of " + msDuration + " msec exceeded time limit of " + allowedTestDuration + " msec",
            msDuration <= allowedTestDuration);
    }

    /**
     * Test if a fitting (in the meaning of the structure) VariantArray can be glued to another one.
     * 
     */
    @Test
    public void testAddVariantArray() {
        final TestArray a = new TestArray(new VariantArray("One", 1, 3, 3));
        final TestArray b = new TestArray(new VariantArray("Two", 3, 3));
        VariantArray array = VariantArray.addValuesToVariantArray(a, b);
        int[] comparing = { 2, 3, 3 };
        assertTrue(Arrays.equals(array.getDimensions(), comparing));

        // Content test
        final TestArray c = new TestArray(new VariantArray("3d dimensions", 3, 4, 5));
        int runner = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 5; k++) {
                    c.setValue(new TypedValue(runner), i, j, k);
                    runner++;
                }
            }
        }

        final TestArray d = new TestArray(new VariantArray("2d dimensions", 4, 5));
        runner = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                d.setValue(new TypedValue(runner), i, j);
                runner++;
            }
        }

        VariantArray result = VariantArray.addValuesToVariantArray(c, d);
        int[] comparing2 = { 4, 4, 5 };
        assertTrue(Arrays.equals(result.getDimensions(), comparing2));

        assertTrue(result.getValue(0, 1, 1).getIntegerValue() == 6);
        final long resultConstantForCheckstyle = 19;
        assertTrue(result.getValue(3, 3, 4).getIntegerValue() == resultConstantForCheckstyle);
    }

    /**
     * Test an expected occurrence of an exception.
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddVariantArrayException() {
        final TestArray a = new TestArray(new VariantArray("Three", 3, 3));
        final TestArray b = new TestArray(new VariantArray("Four", 3, 3));
        VariantArray.addValuesToVariantArray(a, b);
    }

    /**
     * Test an expected occurrence of an exception.
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddVariantArrayException2() {
        final TestArray a = new TestArray(new VariantArray("Five", 1, 3, 3));
        final TestArray b = new TestArray(new VariantArray("Six", 3, 4));
        VariantArray.addValuesToVariantArray(a, b);
    }

    /**
     * Test the correctness of the compression method.
     */
    @Test
    public void testEverything() {
        final float x35 = 3.5f;
        final long x234 = 234L;
        final TestArray a = new TestArray(new VariantArray("y", 3, 3));
        a.setValue(TypedValue.EMPTY, 0, 0);
        a.setValue(TypedValue.EMPTY, 0, 1);
        a.setValue(TypedValue.EMPTY, 0, 2);
        a.setValue(new TypedValue(x35), 1, 0);
        a.setValue(new TypedValue("sdfsd"), 1, 1);
        a.setValue(new TypedValue(true), 1, 2);
        a.setValue(new TypedValue(true), 2, 0);
        a.setValue(new TypedValue(true), 2, 1);
        a.setValue(new TypedValue(true), 2, 2);
        final TestArray b = (TestArray) new TestArray(a).compress();
        assertThat(b.getSize(), is(6)); // compressed size
        b.uncompress();
        for (int i = 0; i < a.getSize(); i++) {
            assertThat(b.getArray()[i].getType(), is(a.getArray()[i].getType()));
            assertThat(b.getArray()[i].getValue(), is(a.getArray()[i].getValue()));
        }

        // try other ending
        a.setValue(new TypedValue(x234), 2, 2);
        final TestArray c = (TestArray) new TestArray(a).compress();
        c.compress();
        assertThat(c.getSize(), is(7)); // compressed size
        c.uncompress();
        for (int i = 0; i < a.getSize(); i++) {
            assertThat(c.getArray()[i].getType(), is(a.getArray()[i].getType()));
            assertThat(c.getArray()[i].getValue(), is(a.getArray()[i].getValue()));
        }
    }

    /**
     * Check the compression of a large array.
     */
    @Test
    public void testLargeArrayCompression() {
        final int x100 = 100;
        final int x1000 = 1000;
        final int x800 = 800;
        final int x799 = 799;
        final double value = 3234.234D;
        final int expectedSize = /* start */2 + /* middle stripes */x800 * 2 + /* frame stripes */x799 * 2 + /* end */2;
        final TestArray a = new TestArray(new VariantArray("a", 1000, 1000));
        for (int i = x100; i < x1000 - x100; i++) {
            for (int j = x100; j < x1000 - x100; j++) {
                a.setValue(new TypedValue(VariableType.Real, Double.toString(value)), i, j);
            }
        }
        final TestArray b = new TestArray(a);
        b.compress();
        assertThat(b.getSize(), is(expectedSize));
        b.uncompress();
        for (int i = 0; i < a.getSize(); i++) {
            assertThat(b.getArray()[i].getType(), is(a.getArray()[i].getType()));
            assertThat(b.getArray()[i].getValue(), is(a.getArray()[i].getValue()));
        }
    }

    /**
     * Check if highest dimension will be cut into slices.
     * 
     */
    @Test
    public void testUnitizeHighestDimension() {
        int runner = 0;

        // 1-dimension test
        final TestArray a = new TestArray(new VariantArray("1 dimension", 3));
        runner = 0;
        for (int i = 0; i < 3; i++) {
            a.setValue(new TypedValue(runner), i);
            runner++;
        }
        List<VariantArray> d1 = a.unitizeHighestDimension();
        assertTrue(d1.size() == 3);
        assertTrue(d1.get(0).getSize() == 1);
        assertTrue(d1.get(1).getSize() == 1);
        assertTrue(d1.get(2).getSize() == 1);
        assertTrue(d1.get(0).getValue(0).getIntegerValue() == 0);
        assertTrue(d1.get(1).getValue(0).getIntegerValue() == 1);
        assertTrue(d1.get(2).getValue(0).getIntegerValue() == 2);

        // 2-dimension test
        final TestArray b = new TestArray(new VariantArray("2 dimensions", 3, 4));
        runner = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                b.setValue(new TypedValue(runner), i, j);
                runner++;
            }
        }
        List<VariantArray> d2 = b.unitizeHighestDimension();
        assertTrue(d2.size() == 3);
        assertTrue(d2.get(0).getSize() == 4);
        assertTrue(d2.get(1).getSize() == 4);
        assertTrue(d2.get(2).getSize() == 4);
        assertTrue(d2.get(0).getValue(2).getIntegerValue() == 2);
        assertTrue(d2.get(2).getValue(2).getIntegerValue() == 10);

        // 3-dimension test
        final TestArray c = new TestArray(new VariantArray("3 dimensions", 3, 4, 5));
        final int resultConstantForCheckstyle = 53;
        runner = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 5; k++) {
                    c.setValue(new TypedValue(runner), i, j, k);
                    runner++;
                }
            }
        }
        List<VariantArray> d3 = c.unitizeHighestDimension();
        assertTrue(d3.size() == 3);
        assertTrue(d3.get(0).getSize() == (4 * 5));
        assertTrue(d3.get(1).getSize() == (4 * 5));
        assertTrue(d3.get(2).getSize() == (4 * 5));
        assertTrue(d3.get(0).getValue(0, 3).getIntegerValue() == 3);
        assertTrue(d3.get(2).getValue(2, 3).getIntegerValue() == resultConstantForCheckstyle);

    }

    /**
     * Check if highest dimension will be pruned.
     * 
     */
    @Test
    public void testPruneHighestDimensionFast() {
        int runner = 0;

        // 1-dimension test
        final TestArray a = new TestArray(new VariantArray("1 dimension", 3));
        runner = 0;
        for (int i = 0; i < 2; i++) {
            a.setValue(new TypedValue(runner), i);
            runner++;
        }
        VariantArray d1 = a.pruneDimensionZero();
        assertTrue(d1.getSize() == 2);
        assertTrue(d1.getValue(d1.getIndex(0)).getIntegerValue() == 0);
        assertTrue(d1.getValue(d1.getIndex(1)).getIntegerValue() == 1);

        // 2-dimension test
        final TestArray b = new TestArray(new VariantArray("2 dimensions", 3, 4));
        runner = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                b.setValue(new TypedValue(runner), i, j);
                runner++;
            }
        }
        VariantArray d2 = b.pruneDimensionZero();
        assertTrue(d2.getSize() == 8);
        assertTrue(d2.getDimensions()[0] == 2);
        assertTrue(d2.getDimensions()[1] == 4);
        int[] tmpTester = { 1, 2 };
        assertTrue(b.getValue(tmpTester).getIntegerValue() == d2.getValue(tmpTester).getIntegerValue());

        // 3-dimension test
        final TestArray c = new TestArray(new VariantArray("3 dimensions", 3, 4, 5));
        final int resultConstantForCheckstyle = 40;
        runner = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    c.setValue(new TypedValue(runner), i, j, k);
                    runner++;
                }
            }
        }
        VariantArray d3 = c.pruneDimensionZero();
        assertTrue(d3.getSize() == resultConstantForCheckstyle);
        assertTrue(d3.getDimensions()[0] == 2);
        assertTrue(d3.getDimensions()[1] == 4);
        assertTrue(d3.getDimensions()[2] == 5);
        int[] tmpTester2 = { 1, 2, 3 };
        assertTrue(c.getValue(tmpTester2).getIntegerValue() == d3.getValue(tmpTester2).getIntegerValue());
    }

    /**
     * Helper class for testing.
     * 
     * @author Arne Bachmann
     */
    class TestArray extends VariantArray {

        private static final long serialVersionUID = 1178953870138931988L;

        TestArray(final VariantArray x) {
            super("a", x);
        }

        TypedValue[] getArray() {
            return getInternalArray();
        }
    }

}

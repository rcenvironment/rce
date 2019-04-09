/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.common;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class containing algorithms for filling up a table with design points.
 * 
 * @author Sascha Zur
 */
public final class DOEAlgorithms {

    /**
     * Constant for the maximum sampling count.
     */
    public static final double MAXMIMAL_RUNS = Math.pow(10, 5);

    private DOEAlgorithms() {

    }

    /**
     * Fills a new table with design points using the full factorial method. This algorithm is
     * recursive.
     * 
     * @param varCount number of variables in the table
     * @param numLevels level count for the precision, must be >= 2
     * @return populated array with the dimensions numLevel^varCount by varCount
     */
    public static Double[][] populateTableFullFactorial(int varCount, int numLevels) {
        if (Math.pow(numLevels, varCount) < MAXMIMAL_RUNS) {
            int noRuns = (int) Math.pow(numLevels, varCount);
            Double[][] table = new Double[noRuns][varCount];
            AtomicInteger run = new AtomicInteger(0);
            populateTableFullFactorialRec(varCount, varCount - 1, numLevels, new double[varCount], run, table);
            return table;
        } else {
            return new Double[0][0];
        }
    }

    private static void populateTableFullFactorialRec(int varCount, int varIndex, int numLevels, double[] values, AtomicInteger run,
        Double[][] table) {
        if (varIndex < 0) {
            for (int i = 0; i < varCount; i++) {
                table[run.get()][i] = values[i];
            }
            run.incrementAndGet();
            values = new double[varCount];
        } else {
            final int lb = -1;
            final int ub = 1;
            double step = (ub - lb) / ((double) numLevels - 1);

            for (int i = 0; i < numLevels; i++) {
                values[varIndex] = lb + i * step;
                populateTableFullFactorialRec(varCount, varIndex - 1, numLevels, values, run, table);
            }
        }
    }

    /**
     * Fills a new table with design points using the latin hypercuve method.
     * 
     * @param varCount number of variables in the table
     * @param desiredRuns number of runs to be calculated
     * @param seed the seed for random number generator
     * @return populated array with the dimensions desiredRuns by varCount
     */
    public static Double[][] populateTableLatinHypercube(int varCount, int desiredRuns, int seed) {
        if (desiredRuns < MAXMIMAL_RUNS) {
            int numRuns = desiredRuns;
            Random generator = new Random(seed);

            double binSize = 2.0 / numRuns;
            int[] iindex = new int[numRuns];
            Double[][] table = new Double[desiredRuns][varCount];
            for (int i = 0; i < varCount; i++) {
                for (int j = 0; j < numRuns; j++) {
                    iindex[j] = j;
                }

                for (int j = 0; j < numRuns; j++) {
                    int k = (int) (generator.nextDouble() * numRuns);
                    int itemp = iindex[k];
                    iindex[k] = iindex[j];
                    iindex[j] = itemp;
                }
                for (int j = 0; j < numRuns; j++) {
                    final double minusOne = -1.0;
                    double val = minusOne + iindex[j] * binSize + generator.nextDouble() * binSize;
                    table[j][i] = val;
                }
            }

            return table;
        } else {
            return new Double[0][0];
        }
    }

    /**
     * 
     * Fills a new table with design points using the monte carlo method.
     * 
     * @param varCount number of variables in the table
     * @param runs number of runs to be calculated
     * @param seed the seed for random number generator
     * @return populated array with the dimensions runs by varCount
     */
    public static Double[][] populateTableMonteCarlo(int varCount, int runs, int seed) {
        if (runs < MAXMIMAL_RUNS) {
            Double[][] result = new Double[runs][varCount];
            Random generator = new Random(seed);
            for (int i = 0; i < runs; i++) {
                for (int j = 0; j < varCount; j++) {
                    result[i][j] = generator.nextDouble() * 2 - 1;
                }
            }
            return result;
        } else {
            return new Double[0][0];
        }
    }

    /**
     * 
     * Transforms the given value from an interval [-1; 1] to the interval [low, up].
     * 
     * @param low lower bound of the target interval
     * @param up upper bound of the target interval
     * @param valueOf relative value in the source interval
     * @return value in the interval [low, up]
     */
    public static Double convertValue(Double low, Double up, Double valueOf) {
        final int minusOne = -1;
        if (valueOf == minusOne) {
            return low;
        } else if (valueOf == 1) {
            return up;
        } else {
            return (((up - low) / 2.0) * valueOf + ((low + up) / 2.0));
        }
    }
}

/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Utility class for verifying program assertions.
 * <p>
 * Please provide useful error messages when using this class. An error message should at least show
 * the name of the parameter which is tested and why it cannot be used in the way the user tries to
 * use it.
 * 
 * @author Andreas Baecker
 * @author Thijs Metsch
 * @author Jens Ruehmkorf
 * @author Heinrich Wendel
 */
public final class Assertions {
    
    /**
     * Hidden constructor for utility class.
     */
    private Assertions() {
        // NOP
    }
    
    /**
     * Verifies that the given parameter evaluates to true.
     * <p>
     * Throws an {@link IllegalArgumentException} if the assertion does not hold.
     * 
     * @param expression the expression to be tested
     * @param message an error message
     * @return assertion status
     */
    public static boolean isTrue(boolean expression, String message) {
        if (!expression) {
            if (message == null || message.trim().isEmpty()) {
                message = "Given boolean value does not evaluate to true!";
            }
            throw new IllegalArgumentException(message);
        }
        return true;
    }
    
    /**
     * Verifies that the given parameter evaluates to false.
     * <p>
     * Throws an {@link IllegalArgumentException} if the assertion does not hold.
     * 
     * @param expression The expression to be tested
     * @param message an error message
     * @return assertion status
     */
    public static boolean isFalse(boolean expression, String message) {
        if (expression) {
            if (message == null || message.trim().isEmpty()) {
                message = "Given boolean value does not evaluate to false!";
            }
            throw new IllegalArgumentException(message);
        }
        return true;
    }
    
    /**
     * Verifies that the values passed as parameters are equal. 
     * <p>
     * Throws an {@link IllegalArgumentException} if the assertion does not hold.
     * 
     * @param expected The expected value
     * @param value The value to test for equality
     * @param message an error message
     * @return assertion status
     */
    public static boolean isEqual(int expected, int value, String message) {
        // FIXME: use "a.compareTo(b) == 0"  instead to allow any type that implements Comparable
        // FIXME: due to autoboxing, int-comparisons could then also be used as parameter
        if (value != expected) {
            if (message == null || message.trim().isEmpty()) {
                message = "Given values are not equal!";
            }
            throw new IllegalArgumentException(message);
        }
        return true;
    }
    
    /**
     * Verifies that the values passed as parameters to methods are bigger than a given
     * barrier. 
     * <p>
     * Throws an {@link IllegalArgumentException} if the assertion does not hold.
     * 
     * @param value the object to test.
     * @param barrier the object has to be bigger than barrier.
     * @param message an error message
     * @return assertion status
     */
    public static boolean isBiggerThan(int value, int barrier, String message) {
        // FIXME: use "a.compareTo(b) > 0"  instead to allow any type that implements Comparable
        // FIXME: due to autoboxing, int-comparisons could then also be used as parameter
        if (!(value > barrier)) {
            if (message == null || message.trim().isEmpty()) {
                message = "The following equation does not hold: [" + value + " > " + barrier + "]!";
            }
            throw new IllegalArgumentException(message);
        }
        return true;
    }

    /**
     * Verifies that the values passed as parameters to methods are bigger than a given
     * barrier. 
     * <p>
     * Throws an {@link IllegalArgumentException} if the assertion does not hold.
     * 
     * @param value the object to test.
     * @param barrier the object has to be bigger than barrier.
     * @param message an error message
     * @return assertion status
     */
    public static boolean isBiggerThan(long value, int barrier, String message) {
        // FIXME: use "a.compareTo(b) > 0"  instead to allow any type that implements Comparable
        // FIXME: due to autoboxing, int-comparisons could then also be used as parameter
        if (!(value > barrier)) {
            if (message == null || message.trim().isEmpty()) {
                message = "The following equation does not hold: [" + value + " > " + barrier + "]!";
            }
            throw new IllegalArgumentException(message);
        }
        return true;
    }

    /**
     * Verifies that the object reference passed as parameter is not <code>null</code>. 
     * In case the passed object reference points to a {@link String} object, this method 
     * verifies also that it does not point to an empty string.
     * <p>
     * Throws an {@link IllegalArgumentException} if the assertion does not hold.
     * 
     * @param object the object reference to test for <code>!null</code>
     * @param message an error message
     * @return assertion status
     */
    public static boolean isDefined(Object object, String message) {
        if (object == null) {
            if (message == null || message.trim().isEmpty()) {
                message = "Given object reference is null!";
            }
            throw new IllegalArgumentException(message);
        }
        if (object instanceof String && ((String) object).trim().isEmpty()) {
            if (message == null || message.trim().isEmpty()) {
                message = "Given string reference points to an empty string!";
            }
            throw new IllegalArgumentException(message);
        }
        return true;
    }

    /**
     * Should be used to verify that references to objects are null. 
     * <p>
     * Throws an {@link IllegalArgumentException} if the assertion does not hold.
     * 
     * @param object the object reference to test for <code>null</code>
     * @param message an error message
     * @return assertion status
     */
    public static boolean isNull(Object object, String message) {
        if (object != null) {
            if (message == null || message.trim().isEmpty()) {
                message = "Given object reference is not null!";
            }
            throw new IllegalArgumentException(message);
        }
        return true;
    }
}

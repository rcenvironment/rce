/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.IOException;
import java.util.List;

import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Test implementation of {@link MethodCallTestInterface}.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class MethodCallTestInterfaceImpl implements MethodCallTestInterface {

    /**
     * Factory method.
     * 
     * @return MethodCallerTestMethods.
     */
    public static MethodCallTestInterface createInstance() {
        return new MethodCallTestInterfaceImpl();
    }

    @Override
    @AllowRemoteAccess
    public String getString() {
        return DEFAULT_RESULT_OR_MESSAGE_STRING;
    }

    @Override
    @Deprecated
    // only included for migration
    public String methodWithoutRemoteAccessPermission() {
        return DEFAULT_RESULT_OR_MESSAGE_STRING;
    }

    /**
     * Static method with no return value.
     */
    public static void dummyFunction() {}

    @Override
    @AllowRemoteAccess
    public int add(Integer a, Integer b) {
        return a + b;
    }

    @Override
    public String objectFunction() {
        return DEFAULT_RESULT_OR_MESSAGE_STRING;
    }

    @Override
    @AllowRemoteAccess
    public void ioExceptionThrower() throws IOException {
        throw new IOException(DEFAULT_RESULT_OR_MESSAGE_STRING);
    }

    @Override
    @AllowRemoteAccess
    public String ioExceptionThrowerWithNonVoidReturnValue() throws IOException {
        throw new IOException(DEFAULT_RESULT_OR_MESSAGE_STRING);
    }

    @Override
    @AllowRemoteAccess
    public void runtimeExceptionThrower() {
        throw new RuntimeException(DEFAULT_RESULT_OR_MESSAGE_STRING);
    }

    @Override
    public String superclass(Object obj1, Object obj2) {
        return obj1.toString() + obj2.toString();
    }

    @Override
    public String list(List<?> list) {
        return DEFAULT_RESULT_OR_MESSAGE_STRING;
    }

    @Override
    public void ambiguous(Object obj1, String string) {

    }

    @Override
    public void ambiguous(String string, Object obj1) {

    }

    @Override
    public Object nullTest(Object test) {
        return null;
    }

    @Override
    @AllowRemoteAccess
    public void callbackTest(Object test) {}

    /**
     * Private and thus not accessible method.
     */
    @SuppressWarnings("unused")
    private void privateFuction() {

    }

}

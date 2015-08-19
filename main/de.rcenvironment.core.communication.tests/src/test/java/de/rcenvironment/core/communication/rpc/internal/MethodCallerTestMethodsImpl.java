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
 * Test implemented methods for the MethodCallerTest.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class MethodCallerTestMethodsImpl implements MethodCallerTestMethods {

    /**
     * Kind of Constructor.
     * 
     * @return MethodCallerTestMethods.
     */
    public static MethodCallerTestMethods getInstance() {
        return new MethodCallerTestMethodsImpl();
    }

    @Override
    public String getValue() {
        return "Hallo Welt";
    }

    /**
     * Static method with no return value.
     */
    public static void dummyFunction() {}

    @Override
    public int add(Integer a, Integer b) {
        return a + b;
    }

    @Override
    public String objectFunction() {
        return "yeah";
    }

    @Override
    public void exceptionFunction() throws IOException {
        throw new IOException("exception");
    }

    @Override
    public void runtimeExceptionFunction() {
        throw new RuntimeException("exception");
    }

    @Override
    public String superclass(Object obj1, Object obj2) {
        return obj1.toString() + obj2.toString();
    }

    @Override
    public String list(List<?> list) {
        return "list";
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

    @Override
    @AllowRemoteAccess
    public String remoteCallAllowed() {
        return getValue();
    }

    /**
     * Private and thus not accessible method.
     */
    @SuppressWarnings("unused")
    private void privateFuction() {

    }

}

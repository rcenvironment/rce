/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static de.rcenvironment.core.communication.rpc.internal.MethodCallTestInterface.DEFAULT_RESULT_OR_MESSAGE_STRING;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheck;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheckHasAnnotation;

/**
 * Unit test for the <code>MethodCaller</code>.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class MethodCallerTest extends TestCase {

    /**
     * Constant.
     */
    private static final String DUMMY_METHOD = "dummyFunction";

    /**
     * Test object for calling methods.
     */
    private MethodCallTestInterface myTestObject;

    @Override
    public void setUp() throws Exception {
        myTestObject = new MethodCallTestInterfaceImpl();
    }

    @Override
    public void tearDown() throws Exception {
        myTestObject = null;
    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallForSuccess() throws Exception {
        MethodCaller.callMethod(myTestObject, DUMMY_METHOD, null);
        assertTrue(true);
    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithReturnValue() throws Exception {
        Object result = MethodCaller.callMethod(myTestObject, "getString", null);
        assertEquals(result, DEFAULT_RESULT_OR_MESSAGE_STRING);
    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithParameters() throws Exception {

        final int a = 2;
        final int b = 3;

        List<Integer> param = new ArrayList<Integer>();
        param.add(a);
        param.add(b);

        final int c = 5;

        Object result = MethodCaller.callMethod(myTestObject, "add", param);
        assertEquals(result, c);

        // call a second time to force usage of cache
        result = MethodCaller.callMethod(myTestObject, "add", param);
        assertEquals(result, c);
    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithObject() throws Exception {
        Object object = MethodCaller.callMethod(myTestObject, "createInstance", null);
        assertNotNull(object);
        Object result = MethodCaller.callMethod(myTestObject, "objectFunction", null);
        assertEquals(result, DEFAULT_RESULT_OR_MESSAGE_STRING);

    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithException() throws Exception {
        try {
            // note: behavior was changed in 7.0.0
            MethodCaller.callMethod(myTestObject, "ioExceptionThrower", null);
            fail("Exception expected");
        } catch (InvocationTargetException e) {
            assertEquals(e.getCause().getClass(), IOException.class);
        }
    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithRuntimeException() throws Exception {
        try {
            // note: behavior was changed in 7.0.0
            MethodCaller.callMethod(myTestObject, "runtimeExceptionThrower", null);
            fail("Exception expected");
        } catch (InvocationTargetException e) {
            assertEquals(e.getCause().getClass(), RuntimeException.class);
        }
    }

    /**
     * Test for failure.
     * 
     * @throws InvocationTargetException on unexpected method exception throws
     */
    public void testCallForFailure() throws InvocationTargetException {

        // No method found
        try {
            MethodCaller.callMethod(myTestObject, "uio", null);
            fail();
        } catch (RemoteOperationException e) {
            assertTrue(true);
        }

        // Wrong parameters
        List<String> parameterList = new ArrayList<String>();
        parameterList.add("wrongParameter");
        try {
            MethodCaller.callMethod(myTestObject, DUMMY_METHOD, parameterList);
            fail();
        } catch (RemoteOperationException e) {
            assertTrue(true);
        }

        // Illegal access
        try {
            MethodCaller.callMethod(myTestObject, "privateFunction", null);
            fail();
        } catch (RemoteOperationException e) {
            assertTrue(true);
        }
    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithSuperclass() throws Exception {
        List<String> params = new ArrayList<String>();
        params.add(new String("hallo"));
        params.add(new String("2"));

        Object result = MethodCaller.callMethod(myTestObject, "superclass", params);
        assertEquals("hallo2", result);
    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithList() throws Exception {
        List<ArrayList<String>> params = new ArrayList<ArrayList<String>>();
        params.add(new ArrayList<String>());

        Object result = MethodCaller.callMethod(myTestObject, "list", params);
        assertEquals(DEFAULT_RESULT_OR_MESSAGE_STRING, result);

    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallNullMethod() throws Exception {
        List<ArrayList<String>> params = new ArrayList<ArrayList<String>>();
        params.add(null);
        Object result = MethodCaller.callMethod(myTestObject, "nullTest", params);
        assertNull(result);

    }

    /**
     * Test for failure.
     * 
     * @throws InvocationTargetException on unexpected method exception throws
     * 
     */
    public void testCallAmbiguous() throws InvocationTargetException {
        List<String> params = new ArrayList<String>();
        params.add(new String());
        params.add(new String());

        try {
            MethodCaller.callMethod(myTestObject, "ambiguous", params);
            fail();
        } catch (RemoteOperationException e) {
            assertTrue(true);
        }
    }

    /**
     * Tests the permission check callback.
     * 
     * @throws RemoteOperationException on unexpected exceptions
     * @throws InvocationTargetException on unexpected method exception throws
     */
    public void testMethodPermission() throws RemoteOperationException, InvocationTargetException {
        // define test methods with and without annotation
        String unmarkedMethodName = "methodWithoutRemoteAccessPermission";
        String markedMethodName = "getString";

        List<String> params = new ArrayList<String>();
        // default: should work
        MethodCaller.callMethod(myTestObject, unmarkedMethodName, params);
        // default: should work
        MethodCaller.callMethod(myTestObject, markedMethodName, params);

        // define permission check: require @AllowRemoteAccess
        MethodPermissionCheck permissionCheck = new MethodPermissionCheckHasAnnotation(AllowRemoteAccess.class);

        // the method without annotation should fail
        try {
            MethodCaller.callMethod(myTestObject, unmarkedMethodName, params, permissionCheck);
            fail();
        } catch (RemoteOperationException e) {
            assertTrue(true);
        }

        // the method with annotation should succeed
        MethodCaller.callMethod(myTestObject, markedMethodName, params, permissionCheck);
    }
}

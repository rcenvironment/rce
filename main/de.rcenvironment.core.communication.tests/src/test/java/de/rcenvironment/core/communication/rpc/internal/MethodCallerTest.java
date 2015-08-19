/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheck;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheckHasAnnotation;

/**
 * Unit test for the <code>MethodCaller</code>.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke (added MethodPermissionCheck test)
 */
public class MethodCallerTest extends TestCase {

    /**
     * Constant.
     */
    private static final String DUMMY_METHOD = "dummyFunction";

    /**
     * Test object for calling methods.
     */
    private MethodCallerTestMethods myTestObject;

    @Override
    public void setUp() throws Exception {
        myTestObject = new MethodCallerTestMethodsImpl();
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
        Object result = MethodCaller.callMethod(myTestObject, "getValue", null);
        assertEquals(result, "Hallo Welt");
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
        Object object = MethodCaller.callMethod(myTestObject, "getInstance", null);
        assertNotNull(object);
        Object result = MethodCaller.callMethod(myTestObject, "objectFunction", null);
        assertEquals(result, "yeah");

    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithException() throws Exception {
        Object result = MethodCaller.callMethod(myTestObject, "exceptionFunction", null);
        assertEquals(result.getClass(), IOException.class);

    }

    /**
     * Test for success.
     * 
     * @throws Exception if the test fails.
     */
    public void testCallWithRuntimeException() throws Exception {
        Object result = MethodCaller.callMethod(myTestObject, "runtimeExceptionFunction", null);
        assertEquals(result.getClass(), RuntimeException.class);

    }

    /**
     * Test for failure.
     * 
     */
    public void testCallForFailure() {

        // No method found
        try {
            MethodCaller.callMethod(myTestObject, "uio", null);
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }

        // Wrong parameters
        List<String> parameterList = new ArrayList<String>();
        parameterList.add("wrongParameter");
        try {
            MethodCaller.callMethod(myTestObject, DUMMY_METHOD, parameterList);
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }

        // Illegal access
        try {
            MethodCaller.callMethod(myTestObject, "privateFunction", null);
            fail();
        } catch (CommunicationException e) {
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
        assertEquals(result, "hallo2");

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
        assertEquals(result, "list");

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
     */
    public void testCallAmbiguous() {
        List<String> params = new ArrayList<String>();
        params.add(new String());
        params.add(new String());

        try {
            MethodCaller.callMethod(myTestObject, "ambiguous", params);
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }
    }

    /**
     * Tests the permission check callback.
     * 
     * @throws CommunicationException on unexpected exceptions
     */
    public void testMethodPermission() throws CommunicationException {
        // define test methods with and without annotation
        String unmarkedMethodName = "getValue";
        String markedMethodName = "remoteCallAllowed";

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
        } catch (CommunicationException e) {
            assertTrue(true);
        }

        // the method with annotation should succeed
        MethodCaller.callMethod(myTestObject, markedMethodName, params, permissionCheck);
    }
}

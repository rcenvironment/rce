/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.IOException;
import java.util.List;

import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.testutils.templates.AbstractCommonVirtualInstanceTest;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheck;

/**
 * Pseudo-service methods being used by {@link MethodCallerTest} and the service call tests in {@link AbstractCommonVirtualInstanceTest}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
@RemotableService
public interface MethodCallTestInterface {

    /**
     * Default String return value, and the default message of thrown Exceptions (for verification by tests).
     */
    String DEFAULT_RESULT_OR_MESSAGE_STRING = "expectedString";

    /**
     * Method with parameters and return value.
     * 
     * @param a first parameter.
     * @param b second parameter.
     * @return int.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    int add(Integer a, Integer b) throws RemoteOperationException;

    /**
     * Method with String return value.
     * 
     * @return the default string
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    String getString() throws RemoteOperationException;

    /**
     * A method for testing {@link MethodPermissionCheck}. The implementation must NOT be annotated with a {@link AllowRemoteAccess}
     * annotation.
     * 
     * @return the default string
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    String methodWithoutRemoteAccessPermission() throws RemoteOperationException;

    /**
     * Dummy non-static function with return value.
     * 
     * @return the default string
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    String objectFunction() throws RemoteOperationException;

    /**
     * Method that throws an exception.
     * 
     * @throws IOException Exception that is thrown.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    void ioExceptionThrower() throws RemoteOperationException, IOException;

    /**
     * Method that throws an exception, and declares a return value.
     * 
     * @return the default string
     * @throws IOException Exception that is thrown.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    String ioExceptionThrowerWithNonVoidReturnValue() throws RemoteOperationException, IOException;

    /**
     * Method that throws a RuntimeException.
     * 
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    void runtimeExceptionThrower() throws RemoteOperationException;

    /**
     * A test function for super classes.
     * 
     * @param obj1 First object.
     * @param obj2 Second object.
     * 
     * @return A String.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    String superclass(Object obj1, Object obj2) throws RemoteOperationException;

    /**
     * Class with a list.
     * 
     * @param list The list.
     * 
     * @return the default string
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    String list(List<?> list) throws RemoteOperationException;

    /**
     * Ambiguous function that overloads with another.
     * 
     * @param obj1 param1.
     * @param string param2.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    void ambiguous(Object obj1, String string) throws RemoteOperationException;

    /**
     * Ambiguous function that overloads with another.
     * 
     * @param obj1 param1.
     * @param string param2.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    void ambiguous(String string, Object obj1) throws RemoteOperationException;

    /**
     * Test serializing null.
     * 
     * @param test A null object.
     * @return A null object.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    Object nullTest(Object test) throws RemoteOperationException;

    /**
     * Test handling of callback object.
     * 
     * @param test A callback object.
     * @throws RemoteOperationException standard exception expected by remote service call tests
     */
    @CallbackMethod
    void callbackTest(Object test) throws RemoteOperationException;

}

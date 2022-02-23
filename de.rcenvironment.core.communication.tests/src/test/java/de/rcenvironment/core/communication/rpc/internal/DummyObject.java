/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Test callback object.
 * 
 * @author Doreen Seider
 */
public class DummyObject implements DummyInterface {

    private static final long serialVersionUID = 1L;

    @Override
    public String someMethod() {
        return "some method called";
    }

    @AllowRemoteAccess
    @Override
    public String someCallbackMethod() {
        return "some callback method called";
    }

    @Override
    public Class<?> getInterface() {
        return DummyInterface.class;
    }

}

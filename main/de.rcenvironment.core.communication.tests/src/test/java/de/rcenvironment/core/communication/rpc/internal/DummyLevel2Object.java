/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

/**
 * Test callback object that does not implement interfaces, but extends a custom superclass.
 * Introduced to prevent regression after fixing https://www.sistec.dlr.de/mantis/view.php?id=6490.
 * 
 * @author Robert Mischke
 */
class DummyLevel2Object extends DummyObject {

    private static final long serialVersionUID = -5558156629463156383L;

    @Override
    public String someCallbackMethod() {
        return super.someCallbackMethod();
    }
}

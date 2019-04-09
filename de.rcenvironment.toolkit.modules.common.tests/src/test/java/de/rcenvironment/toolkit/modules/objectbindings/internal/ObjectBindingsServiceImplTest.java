/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.objectbindings.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsConsumer;

/**
 * Unit tests for {@link ObjectBindingsServiceImpl}.
 * 
 * @author Robert Mischke
 */
public class ObjectBindingsServiceImplTest {

    private static final String TEST_OBJECT_1 = "testObject1";

    private static final String RECEIVED_WRONG_OBJECT_INSTANCE = "Received wrong object instance";

    private ObjectBindingsServiceImpl service;

    private IMocksControl allMocks;

    private StringBindingsConsumer consumerAMock;

    private StringBindingsConsumer consumerBMock;

    /**
     * Artificial interface to create mock consumers.
     * 
     * @author Robert Mischke
     */
    public interface StringBindingsConsumer extends ObjectBindingsConsumer<String> {
    }

    /**
     * Commmon setup.
     */
    @Before
    public void setup() {
        service = new ObjectBindingsServiceImpl(EasyMock.createNiceMock(StatusCollectionRegistry.class));
        allMocks = EasyMock.createStrictControl();
        consumerAMock = allMocks.createMock(StringBindingsConsumer.class);
        consumerBMock = allMocks.createMock(StringBindingsConsumer.class);
    }

    /**
     * Tests various lifecycle steps, adding the first consumer after a binding has already been added.
     */
    @Test
    public void addBindingBeforeConsumerThenReplaceConsumerThenRemoveBinding() {

        service.addBinding(String.class, TEST_OBJECT_1, this);

        // expecting a "pending instance" callback when setting the consumer
        allMocks.reset();
        consumerAMock.addInstance(TEST_OBJECT_1);
        allMocks.replay();

        // verify
        service.setConsumer(String.class, consumerAMock);
        allMocks.verify();

        // when replacing the consumer, expecting a remove callback on A and an add callback on B
        allMocks.reset();
        consumerAMock.removeInstance(TEST_OBJECT_1);
        consumerBMock.addInstance(TEST_OBJECT_1);
        allMocks.replay();

        // verify
        service.setConsumer(String.class, consumerBMock);
        allMocks.verify();

        // expect a "remove" call when unbinding
        allMocks.reset();
        consumerBMock.removeInstance(TEST_OBJECT_1);
        allMocks.replay();

        // verify
        service.removeBinding(String.class, TEST_OBJECT_1);
        allMocks.verify();
    }

    /**
     * Tests various lifecycle steps, adding the first consumer before the first binding is added.
     */
    @Test
    public void addBindingAfterConsumerThenSetConsumerToNull() {

        // not expecting a callback when setting the consumer
        allMocks.reset();
        allMocks.replay();

        // verify
        service.setConsumer(String.class, consumerAMock);
        allMocks.verify();

        // expecting a callback when binding a new instance
        allMocks.reset();
        consumerAMock.addInstance(TEST_OBJECT_1);
        allMocks.replay();

        // verify
        service.addBinding(String.class, TEST_OBJECT_1, this);
        allMocks.verify();

        // expecting a callback when removing the consumer
        allMocks.reset();
        consumerAMock.removeInstance(TEST_OBJECT_1);
        allMocks.replay();

        // verify
        service.setConsumer(String.class, null);
        allMocks.verify();

        // not expecting a callback (as the consumer was already unregistered)
        allMocks.reset();
        allMocks.replay();

        // verify
        service.removeBinding(String.class, TEST_OBJECT_1);
        allMocks.verify();
    }

    /**
     * Tests that different bindings that are considered equal() to each other are handled and removed properly.
     */
    @Test
    public void testUnbindingByIdentityNotEquality() {

        service.setConsumer(String.class, consumerAMock);

        String instance1 = new String(TEST_OBJECT_1);
        String instance2 = new String(TEST_OBJECT_1);
        String instance3 = new String(TEST_OBJECT_1);

        allMocks.reset();
        consumerAMock.addInstance(instance1);
        consumerAMock.addInstance(instance2);
        consumerAMock.addInstance(instance3);
        allMocks.replay();

        service.addBinding(String.class, instance1, this);
        service.addBinding(String.class, instance2, this);
        service.addBinding(String.class, instance3, this);
        allMocks.verify();

        Capture<String> captureX = new Capture<>(); // required as default EasyMock check is by equals()
        Capture<String> captureY = new Capture<>(); // required as default EasyMock check is by equals()

        // test sanity check
        assertTrue(instance1.equals(instance2));
        assertFalse(instance1 == instance2);
        assertTrue(instance3.equals(instance2));
        assertFalse(instance3 == instance2);

        allMocks.reset();
        consumerAMock.removeInstance(EasyMock.capture(captureX));
        allMocks.replay();

        service.removeBinding(String.class, instance2);
        allMocks.verify();
        // note: with the current code, the callback actually uses the provided parameter, so this does not test much
        assertTrue(RECEIVED_WRONG_OBJECT_INSTANCE, captureX.getValue() == instance2);

        // now the actual test: when removing the consumer, it should still "see" the proper remaining instances
        allMocks.reset();
        consumerAMock.removeInstance(EasyMock.capture(captureX));
        consumerAMock.removeInstance(EasyMock.capture(captureY));
        allMocks.replay();

        service.setConsumer(String.class, null);
        allMocks.verify();

        // this should succeed both in the correct and incorrect (equals()) case
        assertTrue(RECEIVED_WRONG_OBJECT_INSTANCE + " (unexpected)", captureY.getValue() == instance3);

        // this should fail if equals() is used, and succeed when identity is used
        assertTrue(RECEIVED_WRONG_OBJECT_INSTANCE, captureX.getValue() == instance1);
    }

}

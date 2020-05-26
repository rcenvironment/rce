/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api.threadcontext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.toolkit.modules.concurrency.internal.AbstractConcurrencyModuleTest;

/**
 * Common test for {@link ThreadContext}, {@link ThreadContextBuilder}, {@link ThreadContextHolder}, and {@link ThreadContextMemento}.
 * 
 * @author Robert Mischke
 */
public class ThreadContextTest extends AbstractConcurrencyModuleTest {

    /**
     * @Before method.
     */
    @Before
    public void resetBefore() {
        ThreadContextHolder.setCurrentContext(null);
    }

    /**
     * Tests basic behavior: Null context handling, context building and deriving, aspect setting and reading, previous context restoration
     * via Memento.
     */
    @Test
    public void basicBehavior() {
        assertNull(ThreadContextHolder.getCurrentContext());
        assertNull(ThreadContextHolder.getCurrentContextAspect(Object.class));
        assertNull(ThreadContextHolder.getCurrentContextAspect(String.class));

        // set new/custom context
        final String value1 = "test data";
        final ThreadContextMemento previousContext1 =
            ThreadContextHolder.setCurrentContext(ThreadContextBuilder.empty().setAspect(String.class, value1).build());
        assertNotNull(ThreadContextHolder.getCurrentContext());
        assertNull(ThreadContextHolder.getCurrentContextAspect(Object.class));
        assertEquals(value1, ThreadContextHolder.getCurrentContextAspect(String.class));

        // set derived context
        final String value2 = value1 + " 2";
        final ThreadContextMemento previousContext2 =
            ThreadContextHolder.setCurrentContext(ThreadContextBuilder.fromCurrent().setAspect(String.class, value2).build());
        assertEquals(value2, ThreadContextHolder.getCurrentContextAspect(String.class));

        // restore first context
        previousContext2.restore();
        assertEquals(value1, ThreadContextHolder.getCurrentContextAspect(String.class));

        // restore original (null) context
        previousContext1.restore();
        assertNull(ThreadContextHolder.getCurrentContext());
        assertNull(ThreadContextHolder.getCurrentContextAspect(Object.class));
        assertNull(ThreadContextHolder.getCurrentContextAspect(String.class));
    }

    /**
     * Verifies that {@link ThreadContextBuilder#fromCurrent()) also works if no ThreadContext has been set yet (so it is null). It is
     * important to test for a non-existing aspect here, as that is the only case where the parent context is actually used.
     */
    @Test
    public void derivingFromNullContext() {
        assertNull(ThreadContextHolder.getCurrentContext());
        final String value = "ok";
        final ThreadContext derivedFromNullContext = ThreadContextBuilder.fromCurrent().setAspect(String.class, value).build();
        assertEquals(value, derivedFromNullContext.getAspect(String.class));
        assertNull(derivedFromNullContext.getAspect(Object.class)); // important test; see JavaDoc
    }

    // TODO add test for Memento consistency check failure
}

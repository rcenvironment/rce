/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;


/**
 * Test cases for {@link ServiceUtils}.
 *
 * @author Doreen Seider
 */
public class ServiceUtilsTest {

    /** Test. */
    @Test
    public void testConstructFilter() {
        Map<String, String> properties = new HashMap<String, String>();
        assertEquals(null, ServiceUtils.constructFilter(properties));
        properties.put("ernie", "bert");
        assertEquals("(&(ernie=bert))", ServiceUtils.constructFilter(properties));
        properties.put("pitti", "platsch");
        assertEquals("(&(pitti=platsch)(ernie=bert))", ServiceUtils.constructFilter(properties));
    }
    
    /** Test. */
    @Test
    public void testCreateNullService() {
        DummyService service = ServiceUtils.createFailingServiceProxy(DummyService.class);
        
        try {
            service.method();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }
    
    /**
     * Test interface.
     *
     * @author Doreen Seider
     */
    interface DummyService {
        
        void method();

    }
    
    /**
     * Test service impl.
     *
     * @author Doreen Seider
     */
    class DummyServiceImpl implements DummyService {

        @Override
        public void method() {
        }
        
    }
}

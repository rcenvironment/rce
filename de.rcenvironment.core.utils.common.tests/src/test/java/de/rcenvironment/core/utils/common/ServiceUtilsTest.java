/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Test cases for {@link ServiceUtils}.
 *
 * @author Doreen Seider
 */
public class ServiceUtilsTest {

    /**
     * Tests if the LDAP filter are constructed as expected from a set of key-value properties. (LDAP filter syntax is used for OSGi service
     * discovery.)
     **/
    @Test
    public void testConstructFilter() {
        String key1 = "key_1";
        String propValue1 = "prop-value_1";
        String key2 = "key_2";
        String propValue2 = "prop-value_2";

        Map<String, String> properties = new HashMap<String, String>();
        assertEquals(null, ServiceUtils.constructFilter(properties));

        properties.put(key1, propValue1);
        String entry1 = StringUtils.format("(%s=%s)", key1, propValue1);
        assertEquals(StringUtils.format("(&%s)", entry1), ServiceUtils.constructFilter(properties));

        properties.put(key2, propValue2);
        String entry2 = StringUtils.format("(%s=%s)", key2, propValue2);

        String filter = ServiceUtils.constructFilter(properties);
        String expectedFilterFormat = "(&%s%s)";
        if (StringUtils.format(expectedFilterFormat, entry1, entry2).equals(filter)
            || StringUtils.format(expectedFilterFormat, entry2, entry1).equals(filter)) {
            assertTrue(true);
        } else {
            fail("Unexpected service filter");
        }
    }

    /** Test. */
    @Test
    public void testCreateNullService() {
        DummyService service = ServiceUtils.createFailingServiceProxy(DummyService.class);

        try {
            service.method();
            fail("Expected that IllegalStateException was thrown");
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    /**
     * Test service interface.
     *
     * @author Doreen Seider
     */
    interface DummyService {

        void method();

    }

    /**
     * Test service implementation.
     *
     * @author Doreen Seider
     */
    class DummyServiceImpl implements DummyService {

        @Override
        public void method() {}

    }
}

/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.testing;

import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class MapMatchers {
    private MapMatchers() {}
    
    public static <T, U> Matcher<Object> map(Matcher<Map<T, U>> matcher) {
        return new BaseMatcher<Object>() {

            @Override
            public boolean matches(Object item) {
                return (item instanceof Map<?, ?>) && matcher.matches(item);
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("is a map");
            }
        };
    }

    public static <T, U> Matcher<Map<T, U>> containsMapping(T key, U value) {
        return containsMapping(CoreMatchers.equalTo(key), CoreMatchers.equalTo(value));
    }
    
    public static <T, U> Matcher<Map<T, U>> containsMapping(Matcher<T> key, U value) {
        return containsMapping(key, CoreMatchers.equalTo(value));
    }
    
    public static <T, U> Matcher<Map<T, U>> containsMapping(T key, Matcher<U> value) {
        return containsMapping(CoreMatchers.equalTo(key), value);
    }
    
    public static <T, U> Matcher<Map<T, U>> containsMapping(Matcher<T> key, Matcher<U> value) {
        return new TypeSafeMatcher<Map<T, U>>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("contains a key-value pair where the key ");
                key.describeTo(arg0);
                arg0.appendText(" and the value ");
                value.describeTo(arg0);
            }

            @Override
            protected boolean matchesSafely(Map<T, U> item) {
                for (Map.Entry<T, U> entry : item.entrySet()) {
                    if (key.matches(entry.getKey()) && value.matches(entry.getValue())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

}

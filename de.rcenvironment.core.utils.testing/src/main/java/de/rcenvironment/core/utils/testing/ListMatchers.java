/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.testing;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class ListMatchers {

    private ListMatchers() {}

    public static <T> Matcher<T> emptyList() {
        return new BaseMatcher<T>() {

            @SuppressWarnings("unchecked")
            @Override
            public boolean matches(Object arg0) {
                return arg0 instanceof List && ((List<T>) arg0).isEmpty();
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("is an empty list");
            }
        };
    }

    public static <T> Matcher<Object> list(Matcher<List<T>> matcher) {
        return new BaseMatcher<Object>() {

            @Override
            public boolean matches(Object item) {
                return (item instanceof List<?>) && matcher.matches(item);
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("is a list");
            }
        };
    }

    @SafeVarargs
    public static <T> Matcher<List<T>> containingInAnyOrder(Matcher<T>... matchers) {
        return new TypeSafeMatcher<List<T>>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendValueList("contains elements that ", " and ", " in any order", matchers);
            }

            @Override
            protected boolean matchesSafely(List<T> arg0) {
                final Collection<Matcher<T>> notYetMatched = new LinkedList<>(Arrays.asList(matchers));
                for (T entry : arg0) {
                    notYetMatched.stream()
                        .filter(matcher -> matcher.matches(entry))
                        .findAny()
                        .ifPresent(notYetMatched::remove);
                }

                return notYetMatched.isEmpty();
            }
        };

    }
}

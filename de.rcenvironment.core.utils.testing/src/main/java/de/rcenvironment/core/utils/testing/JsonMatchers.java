/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonMatchers {
    
    private JsonMatchers() {}

    public static <T> Matcher<String> isJsonList(Matcher<List<T>> matcher) {
        return new TypeSafeMatcher<String>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("is a valid JSON list that");
                matcher.describeTo(description);
            }

            @Override
            protected void describeMismatchSafely(String string, Description mismatchDescription) {
                try {
                    mismatchDescription.appendText("is a valid JSON list that");

                    final List<Object> jsonList = tryParseJsonListFromString(string);
                    matcher.describeMismatch(jsonList, mismatchDescription);
                } catch (JsonProcessingException e) {
                    mismatchDescription.appendText("string \"" + string + "\" does not describe not a valid JSON list");
                }
            }

            @Override
            protected boolean matchesSafely(String string) {
                try {
                    final List<Object> jsonList = tryParseJsonListFromString(string);
                    return matcher.matches(jsonList);
                } catch (JsonProcessingException e) {
                    return false;
                }
            }

            @SuppressWarnings("unchecked")
            private List<Object> tryParseJsonListFromString(final String string)
                throws JsonProcessingException {
                return new ObjectMapper().readValue(string, ArrayList.class);
            }
        };

    }
    
    public static Matcher<String> isJsonObject() {
        return isJsonObject(CoreMatchers.anything());
    }

    public static Matcher<String> isJsonObject(Matcher<? extends Object> matcher) {
        return new TypeSafeMatcher<String>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("is a valid JSON object that");
                matcher.describeTo(description);
            }

            @Override
            protected void describeMismatchSafely(String string, Description mismatchDescription) {
                try {
                    mismatchDescription.appendText("is a valid JSON object that");

                    final Map<String, Object> jsonObject = tryParseJsonObjectFromString(string);
                    matcher.describeMismatch(jsonObject, mismatchDescription);
                } catch (JsonProcessingException e) {
                    mismatchDescription.appendText("string \"" + string + "\" is not a valid JSON object");
                }
            }

            @Override
            protected boolean matchesSafely(String string) {
                try {
                    final Map<String, Object> jsonObject = tryParseJsonObjectFromString(string);
                    return matcher.matches(jsonObject);
                } catch (JsonProcessingException e) {
                    return false;
                }
            }

            @SuppressWarnings("unchecked")
            private Map<String, Object> tryParseJsonObjectFromString(final String string)
                throws JsonProcessingException {
                return new ObjectMapper().readValue(string, HashMap.class);
            }
        };

    }
}

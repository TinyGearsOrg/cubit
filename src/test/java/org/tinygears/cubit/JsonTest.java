/*
 * Copyright (c) 2026 TinyGears.org
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tinygears.cubit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonTest {
    @Test
    void quotesSpecialCharacters() {
        assertEquals("\"a\\\"b\\\\c\\n\\t\\u0001é\"", Json.quote("a\"b\\c\n\t\u0001é"));
    }

    @Test
    void writesAResult() {
        Result r = new Result("6000.0 J", "L^2·M·T^-2", List.of("Energy"), null, false, -1);
        assertEquals(
                "{\"output\":\"6000.0 J\",\"error\":false,\"position\":-1,\"dimension\":\"L^2·M·T^-2\","
                        + "\"assigned\":null,\"types\":[\"Energy\"]}",
                Json.of(r));
        assertEquals(
                "{\"output\":\"boom\",\"error\":true,\"position\":3,\"dimension\":null,\"assigned\":null,\"types\":[]}",
                Json.of(Result.error("boom", 3)));
    }

    @Test
    void parsesAFlatObjectOfStrings() {
        assertEquals(
                Map.of("expr", "3 \"m\"\n\u00e9/", "session", "abc"),
                Json.parseStrings(" { \"expr\" : \"3 \\\"m\\\"\\n\\u00e9\\/\" , \"session\":\"abc\" } "));
        assertEquals(Map.of(), Json.parseStrings("{}"));
    }

    @Test
    void rejectsMalformedInput() {
        for (String bad : new String[] {
            "", "[]", "{", "{\"a\"}", "{\"a\":1}", "{\"a\":\"b\"} x", "{\"a\":\"b\"", "{\"a\":\"\\q\"}",
            "{\"a\":\"\\u12\"}", "{\"a\":\"b", "{\"a\":\"b\\"
        }) {
            assertThrows(IllegalArgumentException.class, () -> Json.parseStrings(bad), bad);
        }
    }
}

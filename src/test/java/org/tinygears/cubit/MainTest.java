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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MainTest {
    private static final String NL = System.lineSeparator();

    private record Run(int code, String out, String err) {}

    private static Run run(String stdin, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(
                args,
                new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void oneShotPrintsTheResult() {
        Run r = run("", "3 kN * 2 m", "to", "J");
        assertEquals(0, r.code());
        assertEquals("6000.0 J" + NL, r.out());
        assertEquals("", r.err());
    }

    @Test
    void oneShotVerbosePrintsTypeAndDimension() {
        assertEquals("6000.0 J" + NL + "  Energy  [L^2·M·T^-2]" + NL, run("", "-v", "3 kN * 2 m to J").out());
    }

    @Test
    void oneShotFailsWithTheErrorOnStderr() {
        Run r = run("", "3 m + 2 s");
        assertEquals(1, r.code());
        assertEquals("", r.out());
        assertEquals("error: cannot add Length [L] and Time [T]" + NL, r.err());
    }

    @Test
    void exactOption() {
        assertEquals("0.3 m" + NL, run("", "--exact", "0.1 m + 0.2 m").out());
    }

    @Test
    void replReadsLinesUntilQuit() {
        Run r = run("x = 2 m\nx * 3\n3 m + 2 s\n:quit\n1 m\n");
        assertEquals(0, r.code());
        assertEquals(
                "x = 2.0 m" + NL + "  Length  [L]" + NL
                        + "6.0 m" + NL + "  Length  [L]" + NL
                        + "error: cannot add Length [L] and Time [T]" + NL,
                r.out());
    }

    @Test
    void replEndsAtEndOfInput() {
        assertEquals(0, run("").code());
        assertEquals("1024.0" + NL, run("2^10\n").out());
    }

    @Test
    void helpAndUsageErrors() {
        assertTrue(run("", "--help").out().startsWith("usage: cubit"));
        assertEquals("--port needs a value" + NL, run("", "--port").err());
        assertEquals("invalid port: abc" + NL, run("", "--serve", "--port", "abc").err());
        assertEquals("invalid port: 70000" + NL, run("", "--serve", "--port", "70000").err());
        assertEquals(2, run("", "--serve", "1 m").code());
        assertEquals("--serve takes no expression and no --units" + NL, run("", "--serve", "1 m").err());
    }

    @Test
    void aMissingUnitsFileIsAnError() {
        Run r = run("", "--units", "/no/such/file.units", "1 m");
        assertEquals(1, r.code());
        assertTrue(r.err().startsWith("error: cannot load /no/such/file.units"), r.err());
    }

    @Test
    void aPortInUseIsAnError() throws Exception {
        try (java.net.ServerSocket taken = new java.net.ServerSocket(0)) {
            Run r = run("", "--serve", "--port", String.valueOf(taken.getLocalPort()));
            assertEquals(1, r.code());
            assertTrue(r.err().startsWith("error: cannot listen on localhost:" + taken.getLocalPort() + ": "), r.err());
            assertEquals("", r.out());
        }
    }

    @Test
    void serveStartsTheServerAndKeepsRunning() throws Exception {
        Run r = run("", "--serve", "--port", "0");
        assertEquals(Main.RUNNING, r.code());
        assertTrue(r.out().startsWith("cubit listening on http://localhost:"), r.out());
    }
}

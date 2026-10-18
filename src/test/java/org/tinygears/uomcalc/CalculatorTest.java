/*
 * Copyright (c) 2026 TinyGears.org
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tinygears.uomcalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculatorTest {
    private Calculator calc;

    @BeforeEach
    void setUp() {
        calc = new Calculator(true);
    }

    private String ok(String line) {
        Result r = calc.evaluate(line);
        assertFalse(r.error(), () -> line + " failed: " + r.output());
        return r.output();
    }

    private Result fail(String line) {
        Result r = calc.evaluate(line);
        assertTrue(r.error(), () -> line + " should fail but gave " + r.output());
        return r;
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '=', value = {
        "3 kN * 2 m = 6000.0 J",
        "3 kN * 2 m to J = 6000.0 J",
        "10 mi / 2 h = 2.2352 m/s",
        "10 mi / 2 h to km/h = 8.04672 km/h",
        "10 mi/h to m/s = 4.4704 m/s",
        "20 °C to °F = 68.0 °F",
        "20 °C to K = 293.15 K",
        "100 °F to °C = 37.77777777777778 °C",
        "3 m + 40 cm = 3.4 m",
        "2^10 = 1024.0",
        "-2^2 = -4.0",
        "2 m^2 = 2.0 m^2",
        "(2 m)^2 = 4.0 m²",
        "2 m ^ 2 to ft^2 = 21.527820833419444 ft^2",
        "1 / 2 m = 0.5 1/m",
        "1 / (2 s) to Hz = 0.5 Hz",
        "1e3 m to km = 1.0 km",
        "9.81 m/s^2 * 70 kg = 686.7 N",
        "9.81 m/s^2 * 70 kg to N = 686.7 N",
        "1 GiB to MiB = 1024.0 MiB",
        "1 gal_US to l = 3.785411784 L",
        "3 km/h = 3.0 km/h",
        "5 m -> cm = 500.0 cm",
        "2 (3 m) = 6.0 m",
        "3 W * 2 s = 6.0 J",
        "2 kg * 3 m/s = 6.0 kg·m/s",
        "1 / (2 s) = 0.5 1/s",
        "6 m / 2 m = 3.0",
    })
    void evaluates(String expression, String expected) {
        assertEquals(expected, ok(expression));
    }

    @Test
    void namesTheTypeAndDimensionOfAProduct() {
        Result r = calc.evaluate("3 kN * 2 m");
        assertEquals(List.of("Energy"), r.types());
        assertEquals("L^2·M·T^-2", r.dimension());
    }

    @Test
    void listsEveryTypeOfASharedDimension() {
        assertEquals(List.of("Frequency", "Radioactivity"), calc.evaluate("1 / (2 s) to Hz").types());
    }

    @Test
    void aNumberHasNoTypeNames() {
        Result r = calc.evaluate("2^10");
        assertEquals("1", r.dimension());
        assertEquals(List.of(), r.types());
    }

    @Test
    void variablesAndAns() {
        Result r = calc.evaluate("x = 90 km/h");
        assertEquals("x = 90.0 km/h", r.output());
        assertEquals("x", r.assigned());
        assertEquals("180.0 km", ok("x * 2 h to km"));
        assertEquals("111.84681460272012 mi", ok("ans to mi"));
        assertEquals("ans = 111.84681460272012 mi\nx = 90.0 km/h", ok(":vars"));
        ok(":clear");
        assertEquals("unknown name 'x'", fail("x").output());
    }

    @Test
    void addingDifferentDimensionsNamesBoth() {
        Result r = fail("3 m + 2 s");
        assertEquals("cannot add Length [L] and Time [T]", r.output());
        assertEquals(4, r.position());
        assertEquals("cannot subtract Length [L] and Time [T]", fail("3 m - 2 s").output());
    }

    @Test
    void convertingToAnIncompatibleUnitSaysSo() {
        Result r = fail("3 m to s");
        assertEquals("cannot convert Length [L] to s [T]", r.output());
        assertEquals(6, r.position());
    }

    @Test
    void reportsUserErrors() {
        assertEquals("unknown name 'foo'", fail("foo + 1").output());
        assertEquals(0, fail("foo + 1").position());
        assertEquals("division by zero", fail("5 / 0").output());
        assertEquals("the exponent must be an integer, not 0.5", fail("2 ^ 0.5").output());
        assertEquals("the exponent must be a number, not Length [L]", fail("2 ^ (3 m)").output());
        assertEquals("exponent out of range (at most 64): 100", fail("2 ^ 100").output());
        assertEquals("'m' is a unit or keyword, choose another name", fail("m = 3").output());
        assertEquals("expected a unit after 'to'", fail("5 km to").output());
        assertEquals("expected ')'", fail("(2 m").output());
        assertEquals("unexpected '*'", fail("2 * * 3").output());
        assertEquals("unexpected character '$'", fail("2 $ 3").output());
        assertEquals("unknown command ':nope', try :help", fail(":nope").output());
    }

    @Test
    void anOverlongInputIsRefused() {
        assertEquals("input too long (at most 1000 characters)", fail("1+".repeat(600)).output());
    }

    @Test
    void emptyInputIsEmptyOutput() {
        assertEquals("", ok("   "));
    }

    @Test
    void exactModeComputesDecimals() {
        assertEquals("exact decimal arithmetic is on", ok(":exact on"));
        assertEquals("0.3 m", ok("0.1 m + 0.2 m"));
        assertEquals("0.3333333333333333333333333333333333 m", ok("1 m / 3"));
        assertEquals("exact decimal arithmetic is off", ok(":exact off"));
        assertEquals("0.30000000000000004 m", ok("0.1 m + 0.2 m"));
        assertEquals("usage: :exact [on|off]", fail(":exact maybe").output());
    }

    @Test
    void searchesTheCatalogs() {
        String out = ok(":units gallon");
        assertTrue(out.contains("gal_US"), out);
        assertTrue(out.contains("USCustomary, Imperial"), out);
        assertEquals("no unit matches 'zzzz'", ok(":units zzzz"));
        assertTrue(ok(":systems").startsWith("SI           128 units"));
    }

    @Test
    void loadsAUnitsFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("medieval.units");
        Files.writeString(file, "@system Medieval\nfurlong:furlong 220 yd\n");
        assertEquals("loaded Medieval (1 units)", ok(":load " + file));
        assertEquals("1.0 furlong", ok("1 furlong"));
        assertEquals("201.168 m", ok("1 furlong to m"));
        assertTrue(ok(":units furlong").contains("Medieval"));
    }

    @Test
    void loadIsOffWhenFilesAreNotAllowed() {
        Result r = new Calculator(false).evaluate(":load /etc/passwd");
        assertTrue(r.error());
        assertEquals(":load is not available here", r.output());
    }

    @Test
    void loadReportsAMissingFile() {
        Result r = fail(":load /no/such/file.units");
        assertTrue(r.output().startsWith("cannot load /no/such/file.units: "), r.output());
    }

    @Test
    void helpListsTheCommands() {
        assertTrue(ok(":help").contains(":units [text]"));
        assertNull(calc.evaluate(":help").assigned());
    }
}

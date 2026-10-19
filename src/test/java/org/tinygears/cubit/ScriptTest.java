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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** The jbang script repeats the dependency versions of the POM; they must not drift apart. */
class ScriptTest {
    private static String pomProperty(String pom, String name) {
        Matcher m = Pattern.compile("<" + Pattern.quote(name) + ">([^<]+)</").matcher(pom);
        assertTrue(m.find(), name + " is not in pom.xml");
        return m.group(1);
    }

    @Test
    void scriptDependenciesMatchThePom() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String script = Files.readString(Path.of("cubit.java"));
        assertTrue(script.contains("//DEPS org.tinygears:uom:" + pomProperty(pom, "uom.version") + "\n"));
        // uom is the only dependency of the application
        assertEquals(1, script.lines().filter(l -> l.startsWith("//DEPS ")).count());
    }

    @Test
    void scriptNamesEverySourceAndResource() throws IOException {
        String script = Files.readString(Path.of("cubit.java"));
        // a pattern does not work when jbang runs the script from a URL
        assertFalse(script.contains("*.java"));
        try (Stream<Path> sources = Files.list(Path.of("src/main/java/org/tinygears/cubit"))) {
            for (Path source : sources.sorted().toList()) {
                String line = "//SOURCES src/main/java/org/tinygears/cubit/" + source.getFileName() + "\n";
                assertTrue(script.contains(line), source.getFileName() + " is not in the script");
            }
        }
        for (String file : new String[] {"web/index.html"}) {
            assertTrue(
                    script.contains("//FILES " + file + "=src/main/resources/" + file + "\n"),
                    file + " is not in the script");
            assertTrue(Files.exists(Path.of("src/main/resources", file)), file);
        }
        assertEquals(
                "org.tinygears.cubit.Main",
                Pattern.compile("(org\\.tinygears\\.cubit\\.Main)\\.main").matcher(script).results()
                        .map(r -> r.group(1)).findFirst().orElse(""));
    }
}

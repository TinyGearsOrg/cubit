///usr/bin/env jbang "$0" "$@" ; exit $?
/*
 * Copyright (c) 2026 TinyGears.org
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
//JAVA 17+
//DEPS org.tinygears:uom:0.1.0
//DEPS io.javalin:javalin:6.7.0
//DEPS org.slf4j:slf4j-simple:2.0.16
//SOURCES src/main/java/org/tinygears/uomcalc/CalcException.java
//SOURCES src/main/java/org/tinygears/uomcalc/Calculator.java
//SOURCES src/main/java/org/tinygears/uomcalc/Evaluator.java
//SOURCES src/main/java/org/tinygears/uomcalc/Json.java
//SOURCES src/main/java/org/tinygears/uomcalc/Lexer.java
//SOURCES src/main/java/org/tinygears/uomcalc/Main.java
//SOURCES src/main/java/org/tinygears/uomcalc/Result.java
//SOURCES src/main/java/org/tinygears/uomcalc/TypeNames.java
//SOURCES src/main/java/org/tinygears/uomcalc/WebServer.java
//FILES web/index.html=src/main/resources/web/index.html
//FILES simplelogger.properties=src/main/resources/simplelogger.properties

/**
 * Runs uomcalc with jbang: {@code jbang uomcalc.java '3 kN * 2 m to J'}. The versions match pom.xml, and every source
 * is listed because jbang cannot expand a pattern when it runs a URL (ScriptTest checks both).
 */
public class uomcalc {
    public static void main(String[] args) {
        org.tinygears.uomcalc.Main.main(args);
    }
}

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
//JAVA 25+
//DEPS org.tinygears:uom:0.1.0
//SOURCES src/main/java/org/tinygears/cubit/CalcException.java
//SOURCES src/main/java/org/tinygears/cubit/Calculator.java
//SOURCES src/main/java/org/tinygears/cubit/Evaluator.java
//SOURCES src/main/java/org/tinygears/cubit/Json.java
//SOURCES src/main/java/org/tinygears/cubit/Lexer.java
//SOURCES src/main/java/org/tinygears/cubit/Main.java
//SOURCES src/main/java/org/tinygears/cubit/Result.java
//SOURCES src/main/java/org/tinygears/cubit/TypeNames.java
//SOURCES src/main/java/org/tinygears/cubit/WebServer.java
//FILES web/index.html=src/main/resources/web/index.html

/**
 * Runs cubit with jbang: {@code jbang cubit.java '3 kN * 2 m to J'}. The versions match pom.xml, and every source
 * is listed because jbang cannot expand a pattern when it runs a URL (ScriptTest checks both).
 */
public class cubit {
    public static void main(String[] args) {
        org.tinygears.cubit.Main.main(args);
    }
}

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
//SOURCES src/main/java/org/tinygears/uomcalc/*.java
//FILES web/index.html=src/main/resources/web/index.html
//FILES simplelogger.properties=src/main/resources/simplelogger.properties

/** Runs uomcalc with jbang: {@code jbang uomcalc.java '3 kN * 2 m to J'}. The versions match pom.xml. */
public class uomcalc {
    public static void main(String[] args) {
        org.tinygears.uomcalc.Main.main(args);
    }
}

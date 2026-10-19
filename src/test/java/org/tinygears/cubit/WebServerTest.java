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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WebServerTest {
    private static WebServer server;
    private static String base;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        server = new WebServer(false).start("localhost", 0);
        base = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    private static HttpResponse<String> post(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/eval"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void evaluatesAnExpression() throws Exception {
        HttpResponse<String> r = post("{\"expr\":\"3 kN * 2 m to J\",\"session\":\"eval-1\"}");
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(
                "{\"output\":\"6000.0 J\",\"error\":false,\"position\":-1,\"dimension\":\"L^2·M·T^-2\","
                        + "\"assigned\":null,\"types\":[\"Energy\"]}",
                r.body());
    }

    @Test
    void anErrorIsStillAnAnswer() throws Exception {
        HttpResponse<String> r = post("{\"expr\":\"3 m + 2 s\",\"session\":\"eval-2\"}");
        assertEquals(200, r.statusCode());
        assertEquals(
                "{\"output\":\"cannot add Length [L] and Time [T]\",\"error\":true,\"position\":4,"
                        + "\"dimension\":null,\"assigned\":null,\"types\":[]}",
                r.body());
    }

    @Test
    void sessionsKeepTheirOwnVariables() throws Exception {
        post("{\"expr\":\"x = 5 m\",\"session\":\"alpha-session\"}");
        assertTrue(post("{\"expr\":\"x * 2\",\"session\":\"alpha-session\"}").body().contains("\"output\":\"10.0 m\""));
        assertTrue(post("{\"expr\":\"x * 2\",\"session\":\"beta-session\"}").body().contains("unknown name 'x'"));
    }

    @Test
    void aSessionCannotReadFiles() throws Exception {
        String body = post("{\"expr\":\":load /etc/passwd\",\"session\":\"files-1\"}").body();
        assertTrue(body.contains(":load is not available here"), body);
    }

    @Test
    void rejectsBadRequests() throws Exception {
        HttpResponse<String> notJson = post("not json");
        assertEquals(400, notJson.statusCode());
        assertTrue(notJson.body().contains("invalid JSON"), notJson.body());
        assertEquals(400, post("{\"session\":\"abc\"}").statusCode());
        HttpResponse<String> badId = post("{\"expr\":\"1\",\"session\":\"../etc\"}");
        assertEquals(400, badId.statusCode());
        assertTrue(badId.body().contains("invalid session id"), badId.body());
        assertEquals(413, post("{\"expr\":\"" + "1".repeat(9000) + "\"}").statusCode());
    }

    @Test
    void servesThePage() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/")).build();
        HttpResponse<String> r = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("<title>cubit</title>"));
    }

    @Test
    void exactModeAppliesToNewSessions() throws Exception {
        WebServer exact = new WebServer(true).start("localhost", 0);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + exact.port() + "/api/eval"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"expr\":\"0.1 m + 0.2 m\",\"session\":\"exact-1\"}"))
                    .build();
            String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(body.contains("\"output\":\"0.3 m\""), body);
        } finally {
            exact.stop();
        }
    }
}

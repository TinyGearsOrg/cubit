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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The web front end on the JDK's HTTP server: a static page and {@code POST /api/eval}, which takes
 * {@code {"expr": "...", "session": "..."}} and answers with the {@link Result} as JSON. Each session id has its own
 * variables; the least recently used sessions are dropped beyond {@link #MAX_SESSIONS}. Sessions cannot read files.
 */
final class WebServer {
    static final int MAX_SESSIONS = 500;
    static final int MAX_BODY = 8192;
    private static final Pattern SESSION_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String JSON = "application/json; charset=utf-8";
    private static final String PAGE = "/web/index.html";

    private final Map<String, Calculator> sessions = new LinkedHashMap<>(16, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Calculator> eldest) {
            return size() > MAX_SESSIONS;
        }
    };
    private final boolean exact;
    private HttpServer server;
    private ExecutorService executor;

    WebServer(boolean exact) {
        this.exact = exact;
    }

    /**
     * Starts listening; {@code port} 0 picks a free port, see {@link #port()}.
     *
     * @throws UncheckedIOException if the address cannot be bound
     */
    WebServer start(String host, int port) {
        byte[] page = page();
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/", exchange -> serve(exchange, page));
        server.createContext("/api/eval", this::eval);
        server.start();
        return this;
    }

    int port() {
        return server.getAddress().getPort();
    }

    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private static byte[] page() {
        try (InputStream in = WebServer.class.getResourceAsStream(PAGE)) {
            if (in == null) {
                throw new IllegalStateException(PAGE + " is not on the class path");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void serve(HttpExchange exchange, byte[] page) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/") && !path.equals("/index.html")) {
            respond(exchange, 404, "text/plain; charset=utf-8", "not found".getBytes(StandardCharsets.UTF_8));
        } else if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            respond(exchange, 405, "text/plain; charset=utf-8", "method not allowed".getBytes(StandardCharsets.UTF_8));
        } else {
            respond(exchange, 200, "text/html; charset=utf-8", page);
        }
    }

    private void eval(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.getResponseHeaders().set("Allow", "POST");
            json(exchange, 405, Result.error("method not allowed, use POST", -1));
            return;
        }
        byte[] raw = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
        if (raw.length > MAX_BODY) {
            json(exchange, 413, Result.error("request too large (at most " + MAX_BODY + " bytes)", -1));
            return;
        }
        Map<String, String> body;
        try {
            body = Json.parseStrings(new String(raw, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            json(exchange, 400, Result.error("invalid JSON: " + e.getMessage(), -1));
            return;
        }
        String expr = body.get("expr");
        String id = body.getOrDefault("session", "default");
        if (expr == null) {
            json(exchange, 400, Result.error("missing \"expr\"", -1));
            return;
        }
        if (!SESSION_ID.matcher(id).matches()) {
            json(exchange, 400, Result.error("invalid session id", -1));
            return;
        }
        Calculator calculator;
        synchronized (sessions) {
            calculator = sessions.computeIfAbsent(id, k -> session());
        }
        json(exchange, 200, calculator.evaluate(expr));
    }

    private Calculator session() {
        Calculator c = new Calculator(false);
        if (exact) {
            c.evaluate(":exact on");
        }
        return c;
    }

    private static void json(HttpExchange exchange, int status, Result result) throws IOException {
        respond(exchange, status, JSON, Json.of(result).getBytes(StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}

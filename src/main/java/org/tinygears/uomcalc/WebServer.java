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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;

/**
 * The web front end: a static page and {@code POST /api/eval}, which takes {@code {"expr": "...", "session": "..."}}
 * and answers with the {@link Result} as JSON. Each session id has its own variables; the least recently used
 * sessions are dropped beyond {@link #MAX_SESSIONS}. Sessions cannot read files.
 */
final class WebServer {
    static final int MAX_SESSIONS = 500;
    private static final Pattern SESSION_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final Map<String, Calculator> sessions = new LinkedHashMap<>(16, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Calculator> eldest) {
            return size() > MAX_SESSIONS;
        }
    };
    private final boolean exact;
    private Javalin app;

    WebServer(boolean exact) {
        this.exact = exact;
    }

    /** Starts listening; {@code port} 0 picks a free port, see {@link #port()}. */
    WebServer start(String host, int port) {
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.http.maxRequestSize = 8192L;
            config.staticFiles.add("/web", Location.CLASSPATH);
        });
        app.post("/api/eval", this::eval);
        app.start(host, port);
        return this;
    }

    int port() {
        return app.port();
    }

    void stop() {
        app.stop();
    }

    private void eval(Context ctx) {
        Map<String, String> body;
        try {
            body = Json.parseStrings(ctx.body());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, "invalid JSON: " + e.getMessage());
            return;
        }
        String expr = body.get("expr");
        String id = body.getOrDefault("session", "default");
        if (expr == null) {
            badRequest(ctx, "missing \"expr\"");
            return;
        }
        if (!SESSION_ID.matcher(id).matches()) {
            badRequest(ctx, "invalid session id");
            return;
        }
        Calculator calculator;
        synchronized (sessions) {
            calculator = sessions.computeIfAbsent(id, k -> session());
        }
        ctx.contentType("application/json").result(Json.of(calculator.evaluate(expr)));
    }

    private Calculator session() {
        Calculator c = new Calculator(false);
        if (exact) {
            c.evaluate(":exact on");
        }
        return c;
    }

    private static void badRequest(Context ctx, String message) {
        ctx.status(HttpStatus.BAD_REQUEST).contentType("application/json").result(Json.of(Result.error(message, -1)));
    }
}

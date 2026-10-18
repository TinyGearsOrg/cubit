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
import java.util.List;
import java.util.Map;

/** The little JSON the web API needs: a flat object of strings in, a result object out. */
final class Json {
    private Json() {}

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    static String of(Result r) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"output\":").append(quote(r.output()));
        sb.append(",\"error\":").append(r.error());
        sb.append(",\"position\":").append(r.position());
        sb.append(",\"dimension\":").append(r.dimension() == null ? "null" : quote(r.dimension()));
        sb.append(",\"assigned\":").append(r.assigned() == null ? "null" : quote(r.assigned()));
        sb.append(",\"types\":");
        appendStrings(sb, r.types());
        return sb.append('}').toString();
    }

    private static void appendStrings(StringBuilder sb, List<String> values) {
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ",").append(quote(values.get(i)));
        }
        sb.append(']');
    }

    /** Parses {@code {"key": "value", ...}} with string values only; other values or shapes are an error. */
    static Map<String, String> parseStrings(String json) {
        Parser p = new Parser(json);
        Map<String, String> out = new LinkedHashMap<>();
        p.skipSpace();
        p.expect('{');
        p.skipSpace();
        if (p.peek() == '}') {
            p.pos++;
            p.end();
            return out;
        }
        while (true) {
            p.skipSpace();
            String key = p.string();
            p.skipSpace();
            p.expect(':');
            p.skipSpace();
            out.put(key, p.string());
            p.skipSpace();
            if (p.peek() == ',') {
                p.pos++;
            } else {
                p.expect('}');
                p.end();
                return out;
            }
        }
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        void skipSpace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        void expect(char c) {
            if (peek() != c) {
                throw new IllegalArgumentException("expected '" + c + "' at " + pos);
            }
            pos++;
        }

        void end() {
            skipSpace();
            if (pos != s.length()) {
                throw new IllegalArgumentException("trailing text at " + pos);
            }
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= s.length()) {
                    throw new IllegalArgumentException("unterminated string");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (pos >= s.length()) {
                    throw new IllegalArgumentException("unterminated escape");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw new IllegalArgumentException("bad \\u escape");
                        }
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw new IllegalArgumentException("bad escape \\" + e);
                }
            }
        }
    }
}

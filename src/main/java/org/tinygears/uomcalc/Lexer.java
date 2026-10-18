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

import java.util.ArrayList;
import java.util.List;

/** Splits an expression into tokens; every token knows its start in the text, for error positions. */
final class Lexer {
    enum Kind { NUMBER, IDENT, OP, END }

    record Token(Kind kind, String text, int start, int end) {}

    private Lexer() {}

    static List<Token> tokenize(String src) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || c == '.' && i + 1 < n && Character.isDigit(src.charAt(i + 1))) {
                int j = number(src, i);
                out.add(new Token(Kind.NUMBER, src.substring(i, j), i, j));
                i = j;
            } else if (isIdentStart(c)) {
                int j = i + 1;
                while (j < n && isIdentPart(src.charAt(j))) {
                    j++;
                }
                out.add(new Token(Kind.IDENT, src.substring(i, j), i, j));
                i = j;
            } else if (c == '-' && i + 1 < n && src.charAt(i + 1) == '>') {
                out.add(new Token(Kind.OP, "->", i, i + 2));
                i += 2;
            } else if ("+-*/^()=·×÷".indexOf(c) >= 0) {
                out.add(new Token(Kind.OP, String.valueOf(c), i, i + 1));
                i++;
            } else {
                throw new CalcException("unexpected character '" + c + "'", i);
            }
        }
        out.add(new Token(Kind.END, "", n, n));
        return out;
    }

    private static int number(String src, int from) {
        int n = src.length();
        int j = from;
        while (j < n && Character.isDigit(src.charAt(j))) {
            j++;
        }
        if (j < n && src.charAt(j) == '.') {
            j++;
            while (j < n && Character.isDigit(src.charAt(j))) {
                j++;
            }
        }
        // an exponent only if digits follow, so "2 e" stays a number and an identifier
        if (j < n && (src.charAt(j) == 'e' || src.charAt(j) == 'E')) {
            int k = j + 1;
            if (k < n && (src.charAt(k) == '+' || src.charAt(k) == '-')) {
                k++;
            }
            if (k < n && Character.isDigit(src.charAt(k))) {
                while (k < n && Character.isDigit(src.charAt(k))) {
                    k++;
                }
                j = k;
            }
        }
        return j;
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '°' || c == 'µ' || c == '%' || c == 'Ω' || c == 'Å';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || Character.isDigit(c);
    }
}

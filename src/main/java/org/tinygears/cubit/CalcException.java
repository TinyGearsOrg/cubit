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

/** A user error in an expression, with the position in the text it refers to (or -1). */
public final class CalcException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int position;

    CalcException(String message, int position) {
        super(message);
        this.position = position;
    }

    CalcException(String message) {
        this(message, -1);
    }

    /** The character offset the message refers to, or -1. */
    public int position() {
        return position;
    }
}

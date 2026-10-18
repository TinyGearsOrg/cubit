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

import java.util.List;

/**
 * The outcome of one input line.
 *
 * @param output the text to show: the formatted quantity, the command output, or the error message
 * @param dimension the dimension of a quantity result, such as {@code L·T^-1}, else null
 * @param types the uom quantity types of that dimension ({@code Frequency}, {@code Radioactivity}), empty if untyped
 * @param assigned the variable the result was assigned to, else null
 * @param error true if {@code output} is an error message
 * @param position the character offset an error refers to, or -1
 */
public record Result(
        String output, String dimension, List<String> types, String assigned, boolean error, int position) {

    static Result text(String output) {
        return new Result(output, null, List.of(), null, false, -1);
    }

    static Result error(String message, int position) {
        return new Result(message, null, List.of(), null, true, position);
    }
}

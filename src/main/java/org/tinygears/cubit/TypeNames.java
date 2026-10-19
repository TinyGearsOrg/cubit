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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.tinygears.uom.Unit;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.engine.Dimension;
import org.tinygears.uom.quantity.QuantityTypes;

/** The uom quantity types of a dimension; several share one (Frequency and Radioactivity). */
final class TypeNames {
    private TypeNames() {}

    private static List<Class<?>> typesOf(Dimension d) {
        return QuantityTypes.types().stream()
                .filter(t -> d.equals(QuantityTypes.dimension(t)))
                .sorted((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()))
                .collect(Collectors.toList());
    }

    /** The simple names of the types with the dimension of {@code q}, empty if it has none (an untyped product). */
    static List<String> namesOf(Untyped q) {
        return typesOf(q.unit().dimension()).stream().map(Class::getSimpleName).collect(Collectors.toList());
    }

    /**
     * {@code q} in the named system unit of its dimension ({@code J}, not {@code kg·m^2/s^2}), which an untyped
     * product does not get by itself. Unchanged if the dimension is 1, has no type, or has types with different
     * system units ({@code Hz} and {@code Bq}), so no unit is picked arbitrarily.
     */
    static Untyped inSystemUnit(Untyped q) {
        Dimension d = q.unit().dimension();
        if (d.equals(Dimension.NONE)) {
            return q;
        }
        Set<Unit<?>> units = typesOf(d).stream().map(QuantityTypes::systemUnit).collect(Collectors.toSet());
        return units.size() == 1 ? q.to(units.iterator().next().asType(Untyped.class)) : q;
    }
}

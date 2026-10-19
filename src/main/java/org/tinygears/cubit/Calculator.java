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
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.tinygears.uom.QuantityFormat;
import org.tinygears.uom.SystemOfUnits;
import org.tinygears.uom.Unit;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.engine.Dimension;
import org.tinygears.uom.systems.CGS;
import org.tinygears.uom.systems.EMU;
import org.tinygears.uom.systems.ESU;
import org.tinygears.uom.systems.Gaussian;
import org.tinygears.uom.systems.Imperial;
import org.tinygears.uom.systems.Information;
import org.tinygears.uom.systems.SI;
import org.tinygears.uom.systems.USCustomary;

/**
 * A calculator session: the variables, the number mode, the loaded unit files and the {@code :} commands.
 * Not thread-safe by itself; {@link #evaluate} is synchronized so a session can serve concurrent web requests.
 */
public final class Calculator {
    static final int MAX_INPUT = 1000;
    private static final int LIST_LIMIT = 40;
    private static final MathContext DECIMALS = MathContext.DECIMAL128;
    private static final List<SystemOfUnits> BUILT_IN = List.of(
            SI.SYSTEM, USCustomary.SYSTEM, Imperial.SYSTEM, CGS.SYSTEM, ESU.SYSTEM, EMU.SYSTEM, Gaussian.SYSTEM,
            Information.SYSTEM);

    private final boolean allowFiles;
    private final Map<String, Untyped> variables = new LinkedHashMap<>();
    private final List<SystemOfUnits> loaded = new ArrayList<>();
    private QuantityFormat quantities = QuantityFormat.standard();
    private UnitFormat units = UnitFormat.standard();
    private boolean exact;

    /**
     * @param allowFiles whether {@code :load} may read files; off for a server, which must not read its own disk
     *     on behalf of a client
     */
    public Calculator(boolean allowFiles) {
        this.allowFiles = allowFiles;
    }

    /** Evaluates one input line: an expression, an assignment or a {@code :} command. */
    public synchronized Result evaluate(String line) {
        String input = line.strip();
        if (input.isEmpty()) {
            return Result.text("");
        }
        if (input.length() > MAX_INPUT) {
            return Result.error("input too long (at most " + MAX_INPUT + " characters)", -1);
        }
        try {
            if (input.startsWith(":")) {
                return command(input.substring(1).strip());
            }
            Evaluator.Outcome outcome = Evaluator.evaluate(this, input);
            Untyped q = outcome.value();
            variables.put("ans", q);
            if (outcome.assigned() != null) {
                variables.put(outcome.assigned(), q);
            }
            String text = quantities.format(q);
            return new Result(
                    outcome.assigned() == null ? text : outcome.assigned() + " = " + text,
                    dimension(q.unit().dimension()),
                    q.unit().dimension().equals(Dimension.NONE) ? List.of() : TypeNames.namesOf(q),
                    outcome.assigned(),
                    false,
                    -1);
        } catch (CalcException e) {
            return Result.error(e.getMessage(), e.position());
        }
    }

    UnitFormat units() {
        return units;
    }

    Untyped variable(String name) {
        return variables.get(name);
    }

    Number number(String text) {
        return exact ? new BigDecimal(text) : Double.valueOf(text);
    }

    Untyped quantity(Number value, Unit<Untyped> unit) {
        return value instanceof BigDecimal b ? Untyped.of(b, unit, DECIMALS) : Untyped.of(value.doubleValue(), unit);
    }

    /** The number 1 in the current mode; a quantity is "plain" if its unit is this one's. */
    Untyped one() {
        return quantity(exact ? BigDecimal.ONE : Double.valueOf(1), units.parse("1").asType(Untyped.class));
    }

    boolean isPlain(Untyped q) {
        return q.unit().equals(one().unit());
    }

    static String dimension(Dimension d) {
        return d.equals(Dimension.NONE) ? "1" : d.toString();
    }

    /** What a quantity is, for an error message: {@code Length [L]}, or just the dimension for an untyped product. */
    static String describe(Untyped q) {
        List<String> names = TypeNames.namesOf(q);
        String dim = dimension(q.unit().dimension());
        return names.isEmpty() ? "[" + dim + "]" : String.join("/", names) + " [" + dim + "]";
    }

    private Result command(String text) {
        String[] parts = text.split("\\s+", 2);
        String name = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].strip() : "";
        switch (name) {
            case "help":
            case "h":
            case "?":
                return Result.text(HELP);
            case "exact":
                return exact(arg);
            case "vars":
                return vars();
            case "clear":
                variables.clear();
                return Result.text("variables cleared");
            case "systems":
                return systems();
            case "units":
                return listUnits(arg);
            case "load":
                return load(arg);
            default:
                return Result.error("unknown command ':" + name + "', try :help", -1);
        }
    }

    private Result exact(String arg) {
        switch (arg.toLowerCase(Locale.ROOT)) {
            case "":
                break;
            case "on":
                exact = true;
                break;
            case "off":
                exact = false;
                break;
            default:
                return Result.error("usage: :exact [on|off]", -1);
        }
        return Result.text("exact decimal arithmetic is " + (exact ? "on" : "off"));
    }

    private Result vars() {
        if (variables.isEmpty()) {
            return Result.text("no variables");
        }
        StringBuilder sb = new StringBuilder();
        variables.forEach((k, v) -> sb.append(k).append(" = ").append(quantities.format(v)).append('\n'));
        return Result.text(sb.toString().stripTrailing());
    }

    private Result systems() {
        StringBuilder sb = new StringBuilder();
        for (SystemOfUnits s : allSystems()) {
            sb.append(String.format("%-12s %d units%n", s.name(), s.units().size()));
        }
        return Result.text(sb.toString().stripTrailing());
    }

    private Result listUnits(String filter) {
        String needle = filter.toLowerCase(Locale.ROOT);
        // one line per unit, with every catalog that has it
        Map<String, List<String>> found = new TreeMap<>();
        for (SystemOfUnits s : allSystems()) {
            for (Unit<?> u : s.units()) {
                String symbol = u.symbol() == null ? "" : u.symbol();
                String unitName = u.name() == null ? "" : u.name();
                if (needle.isEmpty()
                        || symbol.toLowerCase(Locale.ROOT).contains(needle)
                        || unitName.toLowerCase(Locale.ROOT).contains(needle)) {
                    String key = String.format("%-10s %-28s %-14s", symbol, unitName, dimension(u.dimension()));
                    found.computeIfAbsent(key, k -> new ArrayList<>()).add(s.name());
                }
            }
        }
        if (found.isEmpty()) {
            return Result.text("no unit matches '" + filter + "'");
        }
        StringBuilder sb = new StringBuilder();
        found.entrySet().stream()
                .limit(LIST_LIMIT)
                .forEach(e -> sb.append(e.getKey()).append(' ').append(String.join(", ", e.getValue())).append('\n'));
        if (found.size() > LIST_LIMIT) {
            sb.append("... ").append(found.size() - LIST_LIMIT).append(" more, narrow the filter");
        }
        return Result.text(sb.toString().stripTrailing());
    }

    private Result load(String file) {
        if (!allowFiles) {
            return Result.error(":load is not available here", -1);
        }
        if (file.isEmpty()) {
            return Result.error("usage: :load <file.units>", -1);
        }
        try {
            SystemOfUnits system = SystemOfUnits.load(Path.of(file));
            units = units.with(system);
            quantities = new QuantityFormat(units);
            loaded.add(system);
            return Result.text("loaded " + system.name() + " (" + system.units().size() + " units)");
        } catch (IOException | RuntimeException e) {
            return Result.error("cannot load " + file + ": " + e.getMessage(), -1);
        }
    }

    private List<SystemOfUnits> allSystems() {
        List<SystemOfUnits> all = new ArrayList<>(BUILT_IN);
        all.addAll(loaded);
        return all;
    }

    private static final String HELP = """
            Expressions   3 kN * 2 m to J        10 mi / 2 h to km/h       20 °C to °F
            Operators     + - * / ^ ( )          juxtaposition multiplies: 2 (3 m), x y
            Convert       <expr> to <unit>       also: <expr> -> <unit>
            Variables     speed = 90 km/h        the last result is in 'ans'
            Commands      :exact [on|off]   exact decimal arithmetic (0.1 m + 0.2 m is 0.3 m)
                          :vars  :clear     list or clear variables
                          :units [text]     search the unit catalogs by symbol or name
                          :systems          the catalogs of units
                          :load <file>      load a .units file (CLI only)
                          :help  :quit""";
}

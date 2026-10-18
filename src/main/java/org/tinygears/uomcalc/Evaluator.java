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

import java.math.BigDecimal;
import java.util.List;

import org.tinygears.uom.ParseException;
import org.tinygears.uom.Unit;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.Untyped;
import org.tinygears.uomcalc.Lexer.Kind;
import org.tinygears.uomcalc.Lexer.Token;

/**
 * Parses and evaluates one statement over {@link Untyped} quantities:
 *
 * <pre>
 * statement := NAME '=' expr | expr [('to' | '-&gt;') unit]
 * expr      := term (('+' | '-') term)*
 * term      := implicit (('*' | '/') implicit)*
 * implicit  := unary (operand)*          juxtaposition multiplies: {@code 2 (3 m)}, {@code x y}
 * unary     := '-' unary | power
 * power     := primary ['^' unary]       the exponent is an integer number
 * primary   := NUMBER [unit] | NAME | '(' expr ')'
 * </pre>
 *
 * A unit after a number is built with that number ({@code 20 °C} is a point on the Celsius scale, not 20 times
 * {@code 1 °C}); {@code m/s^2} after a number is one unit. Products and quotients of quantities use uom's typed
 * arithmetic, so their results are in system units.
 */
final class Evaluator {
    private static final int MAX_EXPONENT = 64;

    private final Calculator session;
    private final String src;
    private final List<Token> tokens;
    private int pos;

    private Evaluator(Calculator session, String src) {
        this.session = session;
        this.src = src;
        this.tokens = Lexer.tokenize(src);
    }

    /** The result of a statement: the quantity, and the variable it was assigned to or null. */
    record Outcome(Untyped value, String assigned) {}

    static Outcome evaluate(Calculator session, String src) {
        Evaluator e = new Evaluator(session, src);
        try {
            return e.statement();
        } catch (ParseException ex) {
            throw new CalcException(ex.getMessage(), ex.position());
        } catch (ArithmeticException | IllegalArgumentException | org.tinygears.uom.UnitException ex) {
            throw new CalcException(ex.getMessage(), e.peek().start());
        }
    }

    private Outcome statement() {
        Token first = peek();
        if (first.kind() == Kind.IDENT && tokens.get(pos + 1).text().equals("=")) {
            if (isUnit(first.text()) || first.text().equals("to")) {
                throw new CalcException("'" + first.text() + "' is a unit or keyword, choose another name", first.start());
            }
            pos += 2;
            return new Outcome(expression(), first.text());
        }
        Untyped value = expression();
        Token t = peek();
        if (t.kind() == Kind.IDENT && t.text().equals("to") || t.text().equals("->")) {
            String target = src.substring(t.end()).strip();
            if (target.isEmpty()) {
                throw new CalcException("expected a unit after '" + t.text() + "'", t.end());
            }
            Unit<?> unit = session.units().parse(target);
            value = convert(value, unit.asType(Untyped.class), t.end());
            pos = tokens.size() - 1;
        }
        expectEnd();
        return new Outcome(value, null);
    }

    private Untyped convert(Untyped value, Unit<Untyped> unit, int position) {
        if (!value.unit().isCompatible(unit)) {
            throw new CalcException(
                    "cannot convert " + Calculator.describe(value) + " to " + session.units().format(unit)
                            + " [" + Calculator.dimension(unit.dimension()) + "]",
                    position);
        }
        return value.to(unit);
    }

    private void expectEnd() {
        Token t = peek();
        if (t.kind() != Kind.END) {
            throw new CalcException("unexpected '" + t.text() + "'", t.start());
        }
    }

    private Untyped expression() {
        Untyped left = term();
        while (isOp("+") || isOp("-")) {
            Token op = next();
            Untyped right = term();
            left = additive(left, right, op);
        }
        return left;
    }

    private Untyped term() {
        Untyped left = implicit();
        while (isOp("*") || isOp("/") || isOp("·") || isOp("×") || isOp("÷")) {
            Token op = next();
            Untyped right = implicit();
            left = op.text().equals("/") || op.text().equals("÷") ? divide(left, right, op) : multiply(left, right);
        }
        return left;
    }

    private Untyped implicit() {
        Untyped left = unary();
        while (startsOperand()) {
            left = multiply(left, unary());
        }
        return left;
    }

    private boolean startsOperand() {
        Token t = peek();
        return t.kind() == Kind.IDENT && !t.text().equals("to") || t.text().equals("(");
    }

    private Untyped unary() {
        if (isOp("-")) {
            next();
            return unary().negate();
        }
        if (isOp("+")) {
            next();
            return unary();
        }
        return power();
    }

    private Untyped power() {
        Untyped base = primary();
        if (isOp("^")) {
            Token op = next();
            return pow(base, unary(), op);
        }
        return base;
    }

    private Untyped primary() {
        Token t = next();
        switch (t.kind()) {
            case NUMBER: {
                Number n = session.number(t.text());
                Unit<Untyped> unit = unitExpression();
                return session.quantity(n, unit == null ? session.one().unit() : unit);
            }
            case IDENT: {
                Untyped variable = session.variable(t.text());
                if (variable != null) {
                    return variable;
                }
                pos--;
                Unit<Untyped> unit = unitExpression();
                if (unit == null) {
                    throw new CalcException("unknown name '" + t.text() + "'", t.start());
                }
                return session.quantity(session.one().value(), unit);
            }
            case OP:
                if (t.text().equals("(")) {
                    Untyped inner = expression();
                    if (!isOp(")")) {
                        throw new CalcException("expected ')'", peek().start());
                    }
                    next();
                    return inner;
                }
                throw new CalcException("unexpected '" + t.text() + "'", t.start());
            default:
                throw new CalcException("unexpected end of input", t.start());
        }
    }

    /** A unit expression at the current token ({@code km/h}, {@code m/s^2}, {@code kg·(m/s)^2}), or null if none. */
    private Unit<Untyped> unitExpression() {
        int save = pos;
        Unit<Untyped> left = unitFactor();
        if (left == null) {
            pos = save;
            return null;
        }
        while (isOp("*") || isOp("/") || isOp("·")) {
            int mark = pos;
            Token op = next();
            Unit<Untyped> right = unitFactor();
            if (right == null) {
                pos = mark;
                break;
            }
            left = op.text().equals("/") ? left.divide(right) : left.multiply(right);
        }
        return left;
    }

    private Unit<Untyped> unitFactor() {
        Token t = peek();
        Unit<Untyped> unit;
        if (t.kind() == Kind.IDENT && !t.text().equals("to") && session.variable(t.text()) == null) {
            Unit<?> parsed = parseUnit(t.text());
            if (parsed == null) {
                return null;
            }
            next();
            unit = parsed.asType(Untyped.class);
        } else if (t.text().equals("(")) {
            int mark = pos;
            next();
            unit = unitExpression();
            if (unit == null || !isOp(")")) {
                pos = mark;
                return null;
            }
            next();
        } else {
            return null;
        }
        if (isOp("^")) {
            int mark = pos;
            next();
            boolean negative = isOp("-");
            if (negative) {
                next();
            }
            Token e = peek();
            if (e.kind() != Kind.NUMBER || !e.text().chars().allMatch(Character::isDigit)) {
                pos = mark;
                return unit;
            }
            next();
            int n = Integer.parseInt(e.text());
            if (n > MAX_EXPONENT) {
                throw new CalcException("exponent too large: " + n, e.start());
            }
            unit = unit.pow(negative ? -n : n);
        }
        return unit;
    }

    private boolean isUnit(String name) {
        return parseUnit(name) != null;
    }

    private Unit<?> parseUnit(String name) {
        UnitFormat format = session.units();
        try {
            return format.parse(name);
        } catch (ParseException e) {
            return null;
        }
    }

    private Untyped additive(Untyped a, Untyped b, Token op) {
        if (!a.unit().isCompatible(b.unit())) {
            String verb = op.text().equals("+") ? "add" : "subtract";
            throw new CalcException(
                    "cannot " + verb + " " + Calculator.describe(a) + " and " + Calculator.describe(b), op.start());
        }
        return op.text().equals("+") ? a.add(b) : a.subtract(b);
    }

    private Untyped multiply(Untyped a, Untyped b) {
        if (session.isPlain(b)) {
            return a.multiply(b.value());
        }
        if (session.isPlain(a)) {
            return b.multiply(a.value());
        }
        return TypeNames.inSystemUnit(a.multiply(b));
    }

    private Untyped divide(Untyped a, Untyped b, Token op) {
        if (b.value().doubleValue() == 0) {
            throw new CalcException("division by zero", op.start());
        }
        if (session.isPlain(b)) {
            return a.divide(b.value());
        }
        if (session.isPlain(a)) {
            return TypeNames.inSystemUnit(b.inverse().multiply(a.value()));
        }
        return TypeNames.inSystemUnit(a.divide(b));
    }

    private Untyped pow(Untyped base, Untyped exponent, Token op) {
        if (!session.isPlain(exponent)) {
            throw new CalcException("the exponent must be a number, not " + Calculator.describe(exponent), op.start());
        }
        int n = integer(exponent.value(), op);
        if (n == 0) {
            return session.quantity(session.one().value(), session.one().unit());
        }
        Untyped result = base;
        for (int i = 1; i < Math.abs(n); i++) {
            result = multiply(result, base);
        }
        if (n < 0) {
            return divide(session.quantity(session.one().value(), session.one().unit()), result, op);
        }
        return result;
    }

    private static int integer(Number value, Token op) {
        BigDecimal d = value instanceof BigDecimal b ? b : BigDecimal.valueOf(value.doubleValue());
        try {
            int n = d.intValueExact();
            if (Math.abs(n) > MAX_EXPONENT) {
                throw new CalcException("exponent out of range (at most " + MAX_EXPONENT + "): " + n, op.start());
            }
            return n;
        } catch (ArithmeticException e) {
            throw new CalcException("the exponent must be an integer, not " + d.toPlainString(), op.start());
        }
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token next() {
        Token t = tokens.get(pos);
        if (t.kind() != Kind.END) {
            pos++;
        }
        return t;
    }

    private boolean isOp(String text) {
        Token t = peek();
        return t.kind() == Kind.OP && t.text().equals(text);
    }
}

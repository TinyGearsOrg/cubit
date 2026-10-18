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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Entry point: a REPL, a one-shot evaluation, or the web server. */
public final class Main {
    /** Returned by {@link #run} when the web server was started and the process must keep running. */
    static final int RUNNING = -1;

    private static final String USAGE = """
            usage: uomcalc [options] [expression]
              uomcalc                        interactive calculator (:help inside)
              uomcalc '10 mi / 2 h to km/h'  evaluate one expression and print the result
              uomcalc --serve                web UI and API on http://localhost:8080

            options:
              --serve         start the web server
              --host <host>   address to bind with --serve (default localhost)
              --port <port>   port to listen on with --serve (default 8080)
              --exact         exact decimal arithmetic (:exact on)
              --units <file>  load a .units file first (not with --serve)
              -v              in one-shot mode also print the type and dimension
              -h, --help      this text
            """;

    private Main() {}

    public static void main(String[] args) {
        int code = run(args, System.in, System.out, System.err);
        if (code != RUNNING) {
            System.exit(code);
        }
    }

    static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        boolean serve = false;
        boolean exact = false;
        boolean verbose = false;
        String host = "localhost";
        int port = 8080;
        List<String> files = new ArrayList<>();
        List<String> words = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "-h", "--help" -> {
                    out.print(USAGE);
                    return 0;
                }
                case "--serve" -> serve = true;
                case "--exact" -> exact = true;
                case "-v" -> verbose = true;
                case "--host", "--port", "--units" -> {
                    if (i + 1 >= args.length) {
                        err.println(a + " needs a value");
                        return 2;
                    }
                    String value = args[++i];
                    if (a.equals("--host")) {
                        host = value;
                    } else if (a.equals("--units")) {
                        files.add(value);
                    } else {
                        try {
                            port = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            err.println("invalid port: " + value);
                            return 2;
                        }
                        if (port < 0 || port > 65535) {
                            err.println("invalid port: " + value);
                            return 2;
                        }
                    }
                }
                default -> words.add(a);
            }
        }
        if (serve) {
            if (!words.isEmpty() || !files.isEmpty()) {
                err.println("--serve takes no expression and no --units");
                return 2;
            }
            WebServer server = new WebServer(exact).start(host, port);
            out.println("uomcalc listening on http://" + host + ":" + server.port());
            return RUNNING;
        }
        Calculator calculator = new Calculator(true);
        if (exact) {
            calculator.evaluate(":exact on");
        }
        for (String file : files) {
            Result r = calculator.evaluate(":load " + file);
            if (r.error()) {
                err.println("error: " + r.output());
                return 1;
            }
        }
        if (!words.isEmpty()) {
            return oneShot(calculator, String.join(" ", words), verbose, out, err);
        }
        return repl(calculator, in, out, err);
    }

    private static int oneShot(Calculator calculator, String expression, boolean verbose, PrintStream out, PrintStream err) {
        Result r = calculator.evaluate(expression);
        if (r.error()) {
            err.println("error: " + r.output());
            return 1;
        }
        out.println(r.output());
        if (verbose && r.dimension() != null && !detail(r).isEmpty()) {
            out.println(detail(r));
        }
        return 0;
    }

    private static int repl(Calculator calculator, InputStream in, PrintStream out, PrintStream err) {
        boolean interactive = System.console() != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        if (interactive) {
            out.println("uomcalc: units and dimensional analysis, :help for help, :quit to leave");
        }
        try {
            while (true) {
                if (interactive) {
                    out.print("> ");
                    out.flush();
                }
                String line = reader.readLine();
                if (line == null) {
                    return 0;
                }
                String t = line.strip();
                if (t.equals(":quit") || t.equals(":q") || t.equals("quit") || t.equals("exit")) {
                    return 0;
                }
                Result r = calculator.evaluate(line);
                if (r.error()) {
                    if (interactive && r.position() >= 0 && r.position() <= line.length()) {
                        out.println("  " + " ".repeat(r.position()) + "^");
                    }
                    out.println("error: " + r.output());
                } else if (!r.output().isEmpty()) {
                    out.println(r.output());
                    if (r.dimension() != null && !detail(r).isEmpty()) {
                        out.println(detail(r));
                    }
                }
            }
        } catch (IOException e) {
            err.println("error: " + e.getMessage());
            return 1;
        }
    }

    private static String detail(Result r) {
        if (r.dimension().equals("1")) {
            return "";
        }
        String types = r.types().isEmpty() ? "" : String.join(" / ", r.types()) + "  ";
        return "  " + types + "[" + r.dimension() + "]";
    }
}

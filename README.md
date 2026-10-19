<h1 align="center">cubit</h1>

<p align="center"><b>A dimensional-analysis calculator</b><br>
units, typed products and exact conversions, as a command line tool and a web app</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0-blue" alt="License: Apache-2.0"></a>
  <img src="https://img.shields.io/badge/Java-17%2B-orange" alt="Java 17+">
  <a href="https://central.sonatype.com/artifact/org.tinygears/uom"><img src="https://img.shields.io/maven-central/v/org.tinygears/uom?label=uom" alt="uom on Maven Central"></a>
  <a href="https://www.jbang.dev"><img src="https://img.shields.io/badge/run%20with-jbang-green" alt="Run with jbang"></a>
</p>

cubit is a calculator for quantities with units, built on [uom](https://github.com/TinyGearsOrg/uom): it converts
exactly, types the product of two quantities (`Force · Length` is an `Energy`), and reports a sum of different
dimensions as an error that names both. It runs as a command line tool and as a small web app, from one jar. A cubit is
an old unit of length, the span of a forearm.

```
$ cubit '3 kN * 2 m to J'
6000.0 J
$ cubit -v '10 mi / 2 h to km/h'
8.04672 km/h
  Speed  [L·T^-1]
$ cubit '3 m + 2 s'
error: cannot add Length [L] and Time [T]
```

## Build and run

Java 17 or later and Maven 3.9:

```
mvn -q -B clean verify
java -jar target/cubit-0.1.0-SNAPSHOT.jar                  # interactive
java -jar target/cubit-0.1.0-SNAPSHOT.jar '20 °C to °F'    # one expression
java -jar target/cubit-0.1.0-SNAPSHOT.jar --serve          # http://localhost:8080
```

With [jbang](https://www.jbang.dev) no build is needed. The script `cubit.java` takes the same arguments, from a
clone or straight from GitHub:

```
jbang cubit.java '3 kN * 2 m to J'
jbang cubit@TinyGearsOrg/cubit '20 °C to °F'      # the catalog in this repository
jbang https://github.com/TinyGearsOrg/cubit/blob/main/cubit.java --serve
```

jbang asks once whether to trust the repository (`jbang trust add https://github.com/TinyGearsOrg/cubit/`); the
organization in the catalog name is case-sensitive for that check.

The dependency on uom is the released `org.tinygears:uom` from Maven Central.

## The language

| | |
|---|---|
| Units after a number | `3 kN`, `90 km/h`, `9.81 m/s^2`; `20 °C` is a point on the Celsius scale |
| Operators | `+ - * / ^` and parentheses; the exponent is an integer from -64 to 64 |
| Juxtaposition | `2 (3 m)` and `x y` multiply |
| Conversion | `<expression> to <unit>` or `<expression> -> <unit>`, e.g. `10 mi/h to m/s` |
| Variables | `speed = 90 km/h`; `ans` is the last result; a unit symbol cannot be a variable name |
| Exact decimals | `:exact on`: `0.1 m + 0.2 m` is `0.3 m`, with 34 digits for the rest |
| Unit search | `:units gallon`, `:systems`; symbols such as `gal_US`, `mi`, `MiB`, `°F` |
| Your own units | `:load file.units` (CLI), or `--units file.units`; see uom's `docs/CUSTOM-UNITS.md` |

A sum or conversion between different dimensions is an error that names both. A product or quotient is typed by uom
where a relation is declared (`Force · Length` is an `Energy`) and is shown in system units; convert it with `to`.
`-v`, and the web page, also show the quantity types of the result and its dimension: several types can share a
dimension (`Frequency` and `Radioactivity`).

Quote an expression in the shell, which expands `*`: `cubit '2 m * 3'`.

## Web

`--serve` starts a page and `POST /api/eval` with `{"expr": "...", "session": "<id>"}`, which answers
`{"output", "error", "position", "dimension", "types", "assigned"}`. Each session id has its own variables; at most
500 sessions are kept. The server binds to `localhost` unless you pass `--host`, has no authentication and does not
allow `:load`; put it behind a proxy before exposing it.

## License

Apache License 2.0, see [LICENSE](LICENSE).

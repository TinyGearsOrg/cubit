# uomcalc

A dimensional-analysis calculator on [uom](https://github.com/TinyGearsOrg/uom): quantities with units, typed
products, exact conversions. It runs as a command line tool and as a small web app, from one jar.

```
$ uomcalc '3 kN * 2 m to J'
6000.0 J
$ uomcalc -v '10 mi / 2 h to km/h'
8.04672 km/h
  Speed  [L·T^-1]
$ uomcalc '3 m + 2 s'
error: cannot add Length [L] and Time [T]
```

## Build and run

Java 17 or later and Maven 3.9:

```
mvn -q -B clean verify
java -jar target/uom-calc-0.1.0-SNAPSHOT.jar                  # interactive
java -jar target/uom-calc-0.1.0-SNAPSHOT.jar '20 °C to °F'    # one expression
java -jar target/uom-calc-0.1.0-SNAPSHOT.jar --serve          # http://localhost:8080
```

With [jbang](https://www.jbang.dev), no build is needed; the script `uomcalc.java` takes the same arguments:

```
jbang uomcalc.java '3 kN * 2 m to J'
jbang uomcalc.java --serve
jbang uomcalc@tinygearsorg/uom-calc '20 °C to °F'      # from the catalog in this repository
```

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

Quote an expression in the shell, which expands `*`: `uomcalc '2 m * 3'`.

## Web

`--serve` starts a page and `POST /api/eval` with `{"expr": "...", "session": "<id>"}`, which answers
`{"output", "error", "position", "dimension", "types", "assigned"}`. Each session id has its own variables; at most
500 sessions are kept. The server binds to `localhost` unless you pass `--host`, has no authentication and does not
allow `:load`; put it behind a proxy before exposing it.

## License

Apache License 2.0, see [LICENSE](LICENSE).

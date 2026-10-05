# Compiler and stack-machine interpreter

Java 17 implementation of the language used by the reference
`Abstract Machine Compiler/Interpreter` page. The application parses source text,
compiles it to stack-machine instructions, prints their JSON representation and
executes the generated program.

<p align="center">
  <img src="assets/coding-cat-typing.gif" alt="Кот пишет код" width="360">
</p>

Group project by Makar Sinitsyn and Evgeniia Shabas.

## Requirements and tests

- Java 17 or newer;
- no system Maven installation is required; the repository contains Maven Wrapper.

```bash
./mvnw clean test
```

## Run

Compile and execute `example.src`:

```bash
./mvnw -q exec:java -Dexec.args=example.src
```

The program prints both sections shown by the reference site:

```text
JSON representation:
[
  { "CONST": 0 },
  ...
]
Output:
0; 1; 2
```

For a program containing `read(...)`, pass a second file with comma- or
whitespace-separated integers:

```bash
./mvnw -q exec:java -Dexec.args="program.src input.txt"
```

## Source language

The implementation supports the same constructs as the reference site:

```text
x = expression;
x += expression;              -- also -=, *=, /= and %=
read(x);
write(expression);
skip;

if (condition) statement
elif (condition) statement
else statement

while (condition) statement
do statement while (condition);
for (initialization; condition; step) statement

{ statement statement ... }
```

Semicolons after ordinary statements are optional. Blocks must contain at least
one statement. A complete program is one statement, which may itself be a block.

Binary operators, from lowest to highest precedence:

```text
!!
&&
== != < <= > >=
+ -
* / %
```

`!!` is logical OR, as on the reference site. Comparisons and logical operators
produce `1` or `0`. Variables start with a lowercase Latin letter and may then
contain Latin letters, digits and `_`. Integer constants are signed 64-bit values.

Supported comments:

```text
-- until the end of the line
(* block comment *)
```

## Implementation

```text
source text
    -> Lexer
    -> Parser
    -> AST
    -> SourceCompiler
    -> JSON instructions
    -> StackMachine
    -> output values
```

The generated instruction set is `READ`, `WRITE`, `LD`, `ST`, `CONST`, `BINOP`,
`LABEL`, `JMP`, `JZ` and `JNZ`. Label allocation intentionally matches the
reference compiler, including its `L_while_cody_*` spelling.

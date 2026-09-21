# Programming languages platforms and environments

<p align="center">
  <img src="assets/coding-cat-typing.gif" alt="Кот пишет код" width="360">
</p>

Group project by Makar Sinitsyn and Evgeniia Shabas

## Requirements

- Java 17 or newer
- No system Maven installation is required; the repository contains Maven Wrapper

## Build

```bash
./mvnw clean test
```

## Run

```bash
./mvnw -q exec:java -Dexec.args=example.json
```

To read program input from a file, pass it as the second argument:

```bash
./mvnw -q exec:java -Dexec.args="program.json input.txt"
```

Open `pom.xml` as a project in IntelliJ IDEA. The `Main` run configuration uses
`example.json` automatically.

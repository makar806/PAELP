# Programming languages platforms and environments

<p align="center">
  <img src="assets/coding-cat-typing.gif" alt="Кот пишет код" width="360">
</p>

## Requirements

- Java 17 или новее

## Build

```bash
./mvnw clean test
```

## Run

```bash
./mvnw -q exec:java -Dexec.args=example.json
```

Чтобы программа прочитала ввод из файла, укажите его вторым аргументом.

```bash
./mvnw -q exec:java -Dexec.args="program.json input.txt"
```

`Main` run configuration уже использует `example.json` автоматически.

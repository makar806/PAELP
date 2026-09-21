import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private final Map<String, Long> variables = new HashMap<>();
    private final Scanner input;
    private final List<String> output = new ArrayList<>();

    private Main(InputStream input) {
        this.input = new Scanner(input).useDelimiter("[,\\s]+");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java Main <program.json> [input.txt]");
            return;
        }

        JsonElement program;
        try (Reader reader = Files.newBufferedReader(Path.of(args[0]))) {
            program = JsonParser.parseReader(reader);
        }

        Main interpreter;
        if (args.length == 2) {
            try (InputStream inputFile = Files.newInputStream(Path.of(args[1]))) {
                interpreter = new Main(inputFile);
                interpreter.execute(program);
            }
        } else {
            interpreter = new Main(System.in);
            interpreter.execute(program);
        }

        System.out.print(String.join("; ", interpreter.output));
    }

    private void execute(JsonElement node) {
        if (node.isJsonPrimitive() && node.getAsJsonPrimitive().isString()) {
            if (node.getAsString().equals("skip")) return;
            throw fail("Unknown statement: " + node);
        }

        JsonObject statement = asObject(node, "Statement must be an object or \"skip\"");

        if (statement.has("seq")) {
            JsonObject sequence = requiredObject(statement, "seq");
            execute(required(sequence, "left"));
            execute(required(sequence, "right"));
            return;
        }

        if (statement.has("read")) {
            String variable = required(statement, "read").getAsString();
            variables.put(variable, readNumber());
            return;
        }

        if (statement.has("write")) {
            output.add(Long.toString(evaluate(required(statement, "write"))));
            return;
        }

        if (statement.has("assn")) {
            JsonObject assignment = requiredObject(statement, "assn");
            String destination = required(assignment, "dst").getAsString();
            long value = evaluate(required(assignment, "src"));
            variables.put(destination, value);
            return;
        }

        if (statement.has("if")) {
            JsonObject conditional = requiredObject(statement, "if");
            long condition = evaluate(required(conditional, "cond"));
            execute(condition != 0
                    ? required(conditional, "then")
                    : required(conditional, "else"));
            return;
        }

        if (statement.has("while")) {
            JsonObject loop = requiredObject(statement, "while");
            JsonElement condition = required(loop, "cond");
            JsonElement body = required(loop, "body");
            while (evaluate(condition) != 0) {
                execute(body);
            }
            return;
        }

        if (statement.has("do")) {
            JsonObject loop = requiredObject(statement, "do");
            JsonElement body = required(loop, "body");
            JsonElement condition = required(loop, "cond");
            do {
                execute(body);
            } while (evaluate(condition) != 0);
            return;
        }

        throw fail("Unknown statement: " + statement);
    }

    private long evaluate(JsonElement node) {
        JsonObject expression = asObject(node, "Expression must be an object");

        if (expression.has("const")) {
            return required(expression, "const").getAsLong();
        }

        if (expression.has("var")) {
            String name = required(expression, "var").getAsString();
            Long value = variables.get(name);
            if (value == null) throw fail("Undefined variable: " + name);
            return value;
        }

        if (expression.has("binop")) {
            String operation = required(expression, "binop").getAsString();

            long left = evaluate(required(expression, "left"));
            long right = evaluate(required(expression, "right"));
            return apply(operation, left, right);
        }

        throw fail("Unknown expression: " + expression);
    }

    private long apply(String operation, long left, long right) {
        return switch (operation) {
            case "!!" -> bool(truth(left) || truth(right));
            case "&&" -> bool(truth(left) && truth(right));
            case "==" -> bool(left == right);
            case "!=" -> bool(left != right);
            case "<=" -> bool(left <= right);
            case "<" -> bool(left < right);
            case ">=" -> bool(left >= right);
            case ">" -> bool(left > right);
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> {
                if (right == 0) throw fail("Division by zero");
                yield left / right;
            }
            case "%" -> {
                if (right == 0) throw fail("Division by zero");
                yield left % right;
            }
            default -> throw fail("Unknown binary operator: " + operation);
        };
    }

    private long readNumber() {
        if (!input.hasNextLong()) {
            throw fail("read expected an integer in input");
        }
        return input.nextLong();
    }

    private static boolean truth(long value) {
        return value != 0;
    }

    private static long bool(boolean value) {
        return value ? 1 : 0;
    }

    private static JsonObject requiredObject(JsonObject parent, String name) {
        return asObject(required(parent, name), "Field \"" + name + "\" must be an object");
    }

    private static JsonObject asObject(JsonElement value, String message) {
        if (value == null || !value.isJsonObject()) throw fail(message);
        return value.getAsJsonObject();
    }

    private static JsonElement required(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            throw fail("Missing field: " + name);
        }
        return value;
    }

    private static IllegalArgumentException fail(String message) {
        return new IllegalArgumentException(message);
    }
}

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

final class ReferenceJson {
    private static final int WIDTH = 78;

    private ReferenceJson() {
    }

    static JsonObject object(Object... fields) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < fields.length; i += 2) {
            Object value = fields[i + 1];
            result.add((String) fields[i], value instanceof JsonElement json ? json
                    : value instanceof Number number ? new JsonPrimitive(number)
                    : new JsonPrimitive((String) value));
        }
        return result;
    }

    static JsonElement expression(Expression expression) {
        if (expression instanceof ConstantExpression constant) {
            return object("const", constant.value());
        }
        if (expression instanceof VariableExpression variable) {
            return object("var", variable.name());
        }
        if (expression instanceof BinaryExpression binary) {
            return object("binop", binary.operator(), "left", expression(binary.left()),
                    "right", expression(binary.right()));
        }
        if (expression instanceof StringExpression string) {
            return object("string", string.value());
        }
        if (expression instanceof ElementExpression element) {
            return object("elem", expression(element.target()), "index", expression(element.index()));
        }
        if (expression instanceof LengthExpression length) {
            return object("length", expression(length.target()));
        }
        JsonArray arguments = new JsonArray();
        List<Expression> expressions = expression instanceof ArrayExpression array
                ? array.elements() : ((SexpExpression) expression).arguments();
        expressions.forEach(item -> arguments.add(expression(item)));
        return expression instanceof SexpExpression sexp ? object("sexp", sexp.name(), "args", arguments)
                : object("array", arguments);
    }

    static JsonElement statement(Statement statement) {
        if (statement instanceof SkipStatement) {
            return new JsonPrimitive("skip");
        }
        if (statement instanceof ReadStatement read) {
            return object("read", read.variable());
        }
        if (statement instanceof WriteStatement write) {
            return object("write", expression(write.expression()));
        }
        if (statement instanceof AssignmentStatement assignment) {
            return object("assn", object("dst", assignment.variable(), "src", expression(assignment.expression())));
        }
        if (statement instanceof SetStatement set) {
            return object("set", object("dst", expression(set.target()), "idx", expression(set.index()),
                    "value", expression(set.value())));
        }
        if (statement instanceof SequenceStatement sequence) {
            return object("seq", object("left", statement(sequence.first()), "right", statement(sequence.second())));
        }
        if (statement instanceof IfStatement conditional) {
            return object("if", object("cond", expression(conditional.condition()),
                    "then", statement(conditional.thenBranch()), "else", statement(conditional.elseBranch())));
        }
        if (statement instanceof WhileStatement loop) {
            return object("while", object("cond", expression(loop.condition()), "body", statement(loop.body())));
        }
        DoWhileStatement loop = (DoWhileStatement) statement;
        return object("do", object("body", statement(loop.body()), "cond", expression(loop.condition())));
    }

    static String format(JsonElement element) {
        StringBuilder result = new StringBuilder();
        render(element, result, 0, 0);
        return result.toString();
    }

    private static String flat(JsonElement element) {
        if (element.isJsonPrimitive() || element.isJsonNull()) {
            return element.toString();
        }
        List<String> parts = new ArrayList<>();
        if (element.isJsonObject()) {
            element.getAsJsonObject().entrySet().forEach(entry -> parts.add(new JsonPrimitive(entry.getKey())
                    + ": " + flat(entry.getValue())));
        } else {
            element.getAsJsonArray().forEach(item -> parts.add(flat(item)));
        }
        String open = element.isJsonObject() ? "{" : "[";
        String close = element.isJsonObject() ? "}" : "]";
        return parts.isEmpty() ? open + close : open + " " + String.join(", ", parts) + " " + close;
    }

    private static void render(JsonElement element, StringBuilder result, int indentation, int column) {
        String inline = flat(element);
        if (column + inline.length() < WIDTH || element.isJsonPrimitive() || element.isJsonNull()) {
            result.append(inline);
            return;
        }
        boolean object = element.isJsonObject();
        result.append(object ? '{' : '[').append('\n');
        int indent = indentation + 2;
        List<JsonElement> values = new ArrayList<>();
        List<String> prefixes = new ArrayList<>();
        if (object) {
            element.getAsJsonObject().entrySet().forEach(entry -> {
                prefixes.add(new JsonPrimitive(entry.getKey()) + ": ");
                values.add(entry.getValue());
            });
        } else {
            element.getAsJsonArray().forEach(item -> {
                prefixes.add("");
                values.add(item);
            });
        }
        int contentLength = -2;
        for (int i = 0; i < values.size(); i++) {
            contentLength += prefixes.get(i).length() + flat(values.get(i)).length() + 2;
        }
        boolean pack = !object && indent + contentLength < WIDTH;
        result.append(" ".repeat(indent));
        for (int i = 0; i < values.size(); i++) {
            if (i != 0) {
                result.append(pack ? ", " : ",\n" + " ".repeat(indent));
            }
            result.append(prefixes.get(i));
            if (pack) {
                result.append(flat(values.get(i)));
            } else {
                render(values.get(i), result, indent, indent + prefixes.get(i).length());
            }
        }
        result.append('\n').append(" ".repeat(indentation)).append(object ? '}' : ']');
    }
}

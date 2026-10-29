import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

final class Values {
    static final class Text {
        final byte[] bytes;

        Text(String text) {
            bytes = text.getBytes(StandardCharsets.UTF_8);
        }

        String text() {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    static final class Array {
        final List<Object> elements;

        Array(List<?> elements) {
            this.elements = new ArrayList<>(elements);
        }
    }

    static final class Sexp {
        final String name;
        final List<Object> arguments;

        Sexp(String name, List<?> arguments) {
            this.name = name;
            this.arguments = new ArrayList<>(arguments);
        }
    }

    private Values() {
    }

    static Object list(List<Object> elements) {
        Object result = new Sexp("nil", List.of());
        for (int i = elements.size() - 1; i >= 0; i--) {
            result = new Sexp("cons", List.of(elements.get(i), result));
        }
        return result;
    }

    static long integer(Object value) {
        if (value instanceof Long number) {
            return (int) number.longValue();
        }
        throw new LanguageException("ERROR: integer value expected but \"" + format(value) + "\" given");
    }

    static Object variable(java.util.Map<String, Object> state, String name) {
        Object value = state.get(name);
        if (value == null) {
            throw new LanguageException("ERROR: variable \"" + name + "\" is undefined");
        }
        return value;
    }

    static long length(Object value) {
        if (value instanceof Text text) {
            return text.bytes.length;
        }
        if (value instanceof Array array) {
            return array.elements.size();
        }
        if (value instanceof Sexp sexp) {
            return sexp.arguments.size();
        }
        throw new LanguageException("ERROR: array/string/sexp value expected but \""
                + format(value) + "\" given");
    }

    private static int checkedIndex(Object target, Object index) {
        long position = integer(index);
        if (target instanceof Long) {
            throw new LanguageException("ERROR: array/string value expected but \""
                    + format(target) + "\" is given");
        }
        long length = length(target);
        if (position < 0 || position >= length) {
            throw new LanguageException("ERROR: index " + position + " is out of bounds [0, " + length + ")");
        }
        return (int) position;
    }

    static Object element(Object target, Object index) {
        int position = checkedIndex(target, index);
        if (target instanceof Text text) {
            return (long) Byte.toUnsignedInt(text.bytes[position]);
        }
        return target instanceof Array array ? array.elements.get(position)
                : ((Sexp) target).arguments.get(position);
    }

    static void set(Object target, Object index, Object value) {
        int position = checkedIndex(target, index);
        if (target instanceof Text text) {
            long character = integer(value);
            if (character < 0 || character > 255) {
                throw new LanguageException("Invalid_argument(\"Char.chr\")");
            }
            text.bytes[position] = (byte) character;
        } else if (target instanceof Array array) {
            array.elements.set(position, value);
        } else {
            ((Sexp) target).arguments.set(position, value);
        }
    }

    static Object apply(String operator, Object left, Object right) {
        if (operator.equals(":")) {
            throw new LanguageException("File \"_none_\", line 1, characters -1-4: Pattern matching failed");
        }
        if (List.of("==", "!=", "<", "<=", ">", ">=").contains(operator)) {
            String displayed = operator.equals("==") ? "=" : operator.equals("!=") ? "<>" : operator;
            int comparison = compare(displayed, left, right);
            return switch (operator) {
                case "==" -> bool(comparison == 0);
                case "!=" -> bool(comparison != 0);
                case "<" -> bool(comparison < 0);
                case "<=" -> bool(comparison <= 0);
                case ">" -> bool(comparison > 0);
                default -> bool(comparison >= 0);
            };
        }
        int b = (int) integer(right);
        int a = (int) integer(left);
        return switch (operator) {
            case "+" -> (long) (a + b);
            case "-" -> (long) (a - b);
            case "*" -> (long) (a * b);
            case "/", "%" -> {
                if (b == 0) {
                    throw new LanguageException("Division_by_zero");
                }
                yield (long) (operator.equals("/") ? a / b : a % b);
            }
            case "&&" -> bool(a != 0 && b != 0);
            case "!!" -> bool(a != 0 || b != 0);
            default -> throw new LanguageException("undefined operator: " + operator);
        };
    }

    private static int compare(String operator, Object left, Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            return Long.compare(a, b);
        }
        if (left instanceof Text a && right instanceof Text b) {
            return java.util.Arrays.compareUnsigned(a.bytes, b.bytes);
        }
        if (left instanceof Array a && right instanceof Array b) {
            int comparison = Integer.compare(a.elements.size(), b.elements.size());
            for (int i = 0; comparison == 0 && i < a.elements.size(); i++) {
                comparison = compare(operator, a.elements.get(i), b.elements.get(i));
            }
            return comparison;
        }
        throw new LanguageException("ERROR: binary operator \"" + operator + "\" is applied to "
                + format(left) + " and " + format(right));
    }

    private static long bool(boolean value) {
        return value ? 1L : 0L;
    }

    static String output(List<?> values) {
        StringJoiner result = new StringJoiner(", ");
        values.forEach(value -> result.add(format(value)));
        return result.toString();
    }

    static String format(Object value) {
        if (value instanceof Long number) {
            return number.toString();
        }
        if (value instanceof Text text) {
            return "\"" + text.text() + "\"";
        }
        if (value instanceof Array array) {
            return "[" + output(array.elements) + "]";
        }
        Sexp sexp = (Sexp) value;
        if (sexp.name.equals("nil") && sexp.arguments.isEmpty()) {
            return "{}";
        }
        if (sexp.name.equals("cons") && sexp.arguments.size() == 2) {
            List<Object> elements = new ArrayList<>();
            Object tail = sexp;
            while (tail instanceof Sexp cell && cell.name.equals("cons") && cell.arguments.size() == 2) {
                elements.add(cell.arguments.get(0));
                tail = cell.arguments.get(1);
            }
            if (!(tail instanceof Sexp nil) || !nil.name.equals("nil") || !nil.arguments.isEmpty()) {
                throw new LanguageException("File \"_none_\", line 1, characters -1-4: Pattern matching failed");
            }
            return "{" + output(elements) + "}";
        }
        return sexp.name + (sexp.arguments.isEmpty() ? "" : " (" + output(sexp.arguments) + ")");
    }
}

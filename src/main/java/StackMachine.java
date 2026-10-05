import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackMachine {
    private StackMachine() {
    }

    public static List<Long> run(List<Instruction> program, List<Long> suppliedInput) {
        Map<String, Integer> labels = collectLabels(program);
        Map<String, Long> variables = new HashMap<>();
        Deque<Long> stack = new ArrayDeque<>();
        Deque<Long> input = new ArrayDeque<>(suppliedInput);
        List<Long> output = new ArrayList<>();

        int instructionPointer = 0;
        while (instructionPointer < program.size()) {
            Instruction instruction = program.get(instructionPointer++);
            String opcode = instruction.opcode();
            switch (opcode) {
                case "READ" -> {
                    if (input.isEmpty()) {
                        throw new LanguageException("READ: not enough input values");
                    }
                    stack.push(input.removeFirst());
                }
                case "WRITE" -> output.add(pop(stack));
                case "LD" -> {
                    String variable = stringArgument(instruction);
                    Long value = variables.get(variable);
                    if (value == null) {
                        throw new LanguageException("undefined variable: " + variable);
                    }
                    stack.push(value);
                }
                case "ST" -> variables.put(stringArgument(instruction), pop(stack));
                case "CONST" -> stack.push(numberArgument(instruction));
                case "BINOP" -> {
                    long right = pop(stack);
                    long left = pop(stack);
                    stack.push(apply(stringArgument(instruction), left, right));
                }
                case "LABEL" -> {
                    // LABEL only marks a destination.
                }
                case "JMP" -> instructionPointer = resolve(labels, stringArgument(instruction));
                case "JZ" -> {
                    long condition = pop(stack);
                    if (condition == 0) {
                        instructionPointer = resolve(labels, stringArgument(instruction));
                    }
                }
                case "JNZ" -> {
                    long condition = pop(stack);
                    if (condition != 0) {
                        instructionPointer = resolve(labels, stringArgument(instruction));
                    }
                }
                default -> throw new LanguageException("unknown instruction: " + opcode);
            }
        }
        return List.copyOf(output);
    }

    private static Map<String, Integer> collectLabels(List<Instruction> program) {
        Map<String, Integer> labels = new HashMap<>();
        for (int index = 0; index < program.size(); index++) {
            Instruction instruction = program.get(index);
            if (instruction.opcode().equals("LABEL")) {
                String label = stringArgument(instruction);
                if (labels.putIfAbsent(label, index) != null) {
                    throw new LanguageException("duplicate label: " + label);
                }
            }
        }
        return labels;
    }

    private static int resolve(Map<String, Integer> labels, String label) {
        Integer target = labels.get(label);
        if (target == null) {
            throw new LanguageException("undefined label: " + label);
        }
        return target;
    }

    private static long pop(Deque<Long> stack) {
        if (stack.isEmpty()) {
            throw new LanguageException("stack is empty");
        }
        return stack.pop();
    }

    private static String stringArgument(Instruction instruction) {
        if (!(instruction.argument() instanceof String value)) {
            throw new LanguageException(instruction.opcode() + ": string argument expected");
        }
        return value;
    }

    private static long numberArgument(Instruction instruction) {
        if (!(instruction.argument() instanceof Long value)) {
            throw new LanguageException(instruction.opcode() + ": integer argument expected");
        }
        return value;
    }

    private static long apply(String operator, long left, long right) {
        return switch (operator) {
            case "!!" -> bool(left != 0 || right != 0);
            case "&&" -> bool(left != 0 && right != 0);
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
                if (right == 0) {
                    throw new LanguageException("division by zero");
                }
                yield left / right;
            }
            case "%" -> {
                if (right == 0) {
                    throw new LanguageException("division by zero");
                }
                yield left % right;
            }
            default -> throw new LanguageException("undefined operator: " + operator);
        };
    }

    private static long bool(boolean value) {
        return value ? 1 : 0;
    }
}

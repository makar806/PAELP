import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackMachine {
    private StackMachine() {
    }

    public static List<Object> run(List<Instruction> program, List<?> suppliedInput) {
        return runValues(program, suppliedInput);
    }

    public static List<Object> runValues(List<Instruction> program, List<?> suppliedInput) {
        Map<String, Object> variables = new HashMap<>();
        Deque<Object> stack = new ArrayDeque<>();
        Deque<Object> input = new ArrayDeque<>(suppliedInput);
        List<Object> output = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();
        int instructionPointer = 0;
        while (instructionPointer < program.size()) {
            Instruction instruction = program.get(instructionPointer++);
            switch (instruction.opcode()) {
                case "READ" -> {
                    if (input.isEmpty()) {
                        throw new LanguageException("ERROR: input exhausted");
                    }
                    stack.push(input.removeFirst());
                }
                case "WRITE" -> output.add(pop(stack));
                case "LD" -> stack.push(Values.variable(variables, stringArgument(instruction)));
                case "ST" -> variables.put(stringArgument(instruction), pop(stack));
                case "CONST" -> stack.push((long) (int) numberArgument(instruction));
                case "BINOP" -> {
                    Object right = pop(stack);
                    stack.push(Values.apply(stringArgument(instruction), pop(stack), right));
                }
                case "LABEL" -> {
                }
                case "JMP" -> instructionPointer = resolve(program, labels, stringArgument(instruction));
                case "JZ", "JNZ" -> {
                    long condition = Values.integer(pop(stack));
                    if ((condition == 0) == instruction.opcode().equals("JZ")) {
                        instructionPointer = resolve(program, labels, stringArgument(instruction));
                    }
                }
                case "STRING" -> stack.push(new Values.Text(stringArgument(instruction)));
                case "ARRAY" -> stack.push(new Values.Array(popArguments(stack, numberArgument(instruction))));
                case "SEXP" -> {
                    if (!(instruction.argument() instanceof Instruction.SexpArgument sexp)) {
                        throw new LanguageException("SEXP: name and arity expected");
                    }
                    stack.push(new Values.Sexp(sexp.name(), popArguments(stack, sexp.arity())));
                }
                case "LENGTH" -> stack.push(Values.length(pop(stack)));
                case "ELEM" -> {
                    Object index = pop(stack);
                    stack.push(Values.element(pop(stack), index));
                }
                case "SET" -> {
                    Object value = pop(stack);
                    Object index = pop(stack);
                    Values.set(pop(stack), index, value);
                }
                default -> throw new LanguageException("unknown instruction: " + instruction.opcode());
            }
        }
        return List.copyOf(output);
    }

    private static int resolve(List<Instruction> program, Map<String, Integer> labels, String label) {
        if (labels.isEmpty()) {
            for (int index = 0; index < program.size(); index++) {
                Instruction instruction = program.get(index);
                if (instruction.opcode().equals("LABEL")) {
                    String name = stringArgument(instruction);
                    if (labels.putIfAbsent(name, index + 1) != null) {
                        throw new LanguageException("ERROR: duplicate label \"" + name + "\"");
                    }
                }
            }
        }
        Integer target = labels.get(label);
        if (target == null) {
            throw new LanguageException("ERROR: indefined label \"" + label + "\"");
        }
        return target;
    }

    private static List<Object> popArguments(Deque<Object> stack, long count) {
        if (count < 0 || count > stack.size()) {
            throw new LanguageException("ERROR: stack exhausted");
        }
        List<Object> arguments = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            arguments.add(pop(stack));
        }
        Collections.reverse(arguments);
        return arguments;
    }

    private static Object pop(Deque<Object> stack) {
        if (stack.isEmpty()) {
            throw new LanguageException("ERROR: stack exhausted");
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
}

import java.util.List;

public final class ReferenceEngine {
    private ReferenceEngine() {
    }

    public static ReferenceResult evaluate(String source, String input) {
        return run(source, input, false);
    }

    public static ReferenceResult compileAndRun(String source, String input) {
        return run(source, input, true);
    }

    public static ReferenceResult evaluate(String source, List<?> input) {
        return run(source, input, false);
    }

    public static ReferenceResult compileAndRun(String source, List<?> input) {
        return run(source, input, true);
    }

    private static ReferenceResult run(String source, List<?> input, boolean machine) {
        try {
            return execute(Parser.parse(source), input, machine);
        } catch (LanguageException exception) {
            return new ReferenceResult("", "", exception.getMessage());
        }
    }

    private static ReferenceResult run(String source, String input, boolean machine) {
        Statement program;
        List<Object> values;
        try {
            program = Parser.parse(source);
        } catch (LanguageException exception) {
            return new ReferenceResult("", "", exception.getMessage());
        }
        try {
            values = Parser.parseInput(input);
        } catch (LanguageException exception) {
            return new ReferenceResult("", "", "Input error: " + exception.getMessage());
        }
        return execute(program, values, machine);
    }

    private static ReferenceResult execute(Statement program, List<?> values, boolean machine) {
        List<Instruction> instructions = machine ? SourceCompiler.compile(program) : List.of();
        String json = machine ? MachineProgramJson.serialize(instructions)
                : ReferenceJson.format(ReferenceJson.statement(program));
        try {
            List<Object> output = machine ? StackMachine.runValues(instructions, values)
                    : SourceInterpreter.run(program, values);
            return new ReferenceResult(json, Values.output(output), "");
        } catch (LanguageException exception) {
            return new ReferenceResult(json, "", exception.getMessage());
        }
    }
}

import java.util.List;

public final class LanguageEngine {
    private LanguageEngine() {
    }

    public static EngineResult compileAndRun(String source, List<?> input) {
        List<Instruction> instructions = SourceCompiler.compile(source);
        String json = MachineProgramJson.serialize(instructions);
        List<Object> output = StackMachine.runValues(instructions, input);
        return new EngineResult(instructions, json, output);
    }
}

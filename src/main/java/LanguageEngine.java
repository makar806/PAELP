import java.util.List;

public final class LanguageEngine {
    private LanguageEngine() {
    }

    public static EngineResult compileAndRun(String source, List<Long> input) {
        List<Instruction> instructions = SourceCompiler.compile(source);
        String json = MachineProgramJson.serialize(instructions);
        List<Long> output = StackMachine.run(instructions, input);
        return new EngineResult(instructions, json, output);
    }
}

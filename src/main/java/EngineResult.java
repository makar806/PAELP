import java.util.List;

public record EngineResult(List<Instruction> instructions, String json, List<Object> output) {
    public EngineResult {
        instructions = List.copyOf(instructions);
        output = List.copyOf(output);
    }
}

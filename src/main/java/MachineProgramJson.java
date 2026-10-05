import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class MachineProgramJson {
    private static final int INLINE_WIDTH = 80;

    private MachineProgramJson() {
    }

    public static String serialize(List<Instruction> program) {
        if (program.isEmpty()) {
            return "[]";
        }
        List<String> encoded = new ArrayList<>(program.size());
        for (Instruction instruction : program) {
            encoded.add(serializeInstruction(instruction));
        }

        String inline = "[ " + String.join(", ", encoded) + " ]";
        if (inline.length() <= INLINE_WIDTH) {
            return inline;
        }
        return "[\n"
                + encoded.stream().map(value -> "  " + value).collect(Collectors.joining(",\n"))
                + "\n]";
    }

    private static String serializeInstruction(Instruction instruction) {
        if (instruction.argument() == null) {
            return quote(instruction.opcode());
        }
        return "{ " + quote(instruction.opcode()) + ": " + serializeArgument(instruction.argument()) + " }";
    }

    private static String serializeArgument(Object argument) {
        if (argument instanceof Long number) {
            return Long.toString(number);
        }
        return quote((String) argument);
    }

    private static String quote(String value) {
        return new JsonPrimitive(value).toString();
    }
}

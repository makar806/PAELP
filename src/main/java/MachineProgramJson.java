import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import java.util.List;

public final class MachineProgramJson {
    private MachineProgramJson() {
    }

    public static String serialize(List<Instruction> program) {
        JsonArray encoded = new JsonArray();
        for (Instruction instruction : program) {
            Object argument = instruction.argument();
            if (argument == null) {
                encoded.add(new JsonPrimitive(instruction.opcode()));
            } else if (argument instanceof Instruction.SexpArgument sexp) {
                JsonArray arguments = new JsonArray();
                arguments.add(sexp.name());
                arguments.add(sexp.arity());
                encoded.add(ReferenceJson.object("SEXP", arguments));
            } else {
                encoded.add(ReferenceJson.object(instruction.opcode(), argument));
            }
        }
        return ReferenceJson.format(encoded);
    }
}

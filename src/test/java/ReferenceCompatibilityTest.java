import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceCompatibilityTest {
    @TestFactory
    Stream<DynamicTest> matchesReferencePages() {
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/reference-cases.json")), StandardCharsets.UTF_8);
        JsonArray cases = JsonParser.parseReader(reader).getAsJsonArray();
        return StreamSupport.stream(cases.spliterator(), false).flatMap(item -> {
            JsonObject test = item.getAsJsonObject();
            String source = test.get("source").getAsString();
            String input = test.get("input").getAsString();
            return Stream.of("eval", "sm").map(mode -> DynamicTest.dynamicTest(
                    mode + ": " + source + " input=" + input,
                    () -> {
                        ReferenceResult actual = mode.equals("eval")
                                ? ReferenceEngine.evaluate(source, input)
                                : ReferenceEngine.compileAndRun(source, input);
                        JsonObject expected = test.getAsJsonObject(mode);
                        assertEquals(expected.get("message").getAsString(), actual.message());
                        assertEquals(expected.get("res").getAsString(), actual.output());
                        assertEquals(expected.get("output").getAsString(), actual.json());
                    }));
        });
    }

    @Test
    void exposesBothCommandLineModes(@TempDir Path directory) throws Exception {
        Path program = directory.resolve("program.src");
        Path input = directory.resolve("input.txt");
        Files.writeString(program, "{ read(x); x[0] += 1; write(x); }");
        Files.writeString(input, "[20, 22]");
        for (String mode : List.of("eval", "sm")) {
            String result = capture(new String[]{mode, program.toString(), input.toString()});
            ReferenceResult expected = mode.equals("eval")
                    ? ReferenceEngine.evaluate(Files.readString(program), Files.readString(input))
                    : ReferenceEngine.compileAndRun(Files.readString(program), Files.readString(input));
            assertEquals("JSON representation:\n" + expected.json() + "\nOutput:\n[21, 22]\n", result);
        }
        Files.writeString(program, "write(42);");
        assertEquals("JSON representation:\n[ { \"CONST\": 42 }, \"WRITE\" ]\nOutput:\n42\n",
                capture(new String[]{program.toString()}));
    }

    @Test
    void reportsMachineFailuresAndResolvesLabelsOnlyWhenJumping() {
        assertEquals(List.of(), StackMachine.runValues(List.of(
                Instruction.withString("LABEL", "x"), Instruction.withString("LABEL", "x")), List.of()));
        assertFailure("ERROR: duplicate label \"x\"", List.of(
                Instruction.withString("LABEL", "x"), Instruction.withString("LABEL", "x"),
                Instruction.withString("JMP", "x")));
        assertFailure("ERROR: indefined label \"missing\"", List.of(Instruction.withString("JMP", "missing")));
        for (String opcode : List.of("WRITE", "ST", "BINOP", "JZ", "JNZ", "LENGTH", "ELEM", "SET")) {
            assertFailure("ERROR: stack exhausted", List.of(Instruction.withString(opcode, "x")));
        }
    }

    private static void assertFailure(String message, List<Instruction> instructions) {
        assertEquals(message, assertThrows(LanguageException.class,
                () -> StackMachine.runValues(instructions, List.of())).getMessage());
    }

    private static String capture(String[] args) throws Exception {
        PrintStream previous = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            Main.run(args);
        } finally {
            System.setOut(previous);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}

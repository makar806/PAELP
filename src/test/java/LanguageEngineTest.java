import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageEngineTest {
    @Test
    void compilesAndRunsForExactlyLikeTheReferenceSite() {
        String source = """
                for (x = 0; x < 3; x = x + 1) {
                    write(x);
                }
                """;

        EngineResult result = LanguageEngine.compileAndRun(source, List.of());

        assertEquals(List.of(
                Instruction.withNumber("CONST", 0),
                Instruction.withString("ST", "x"),
                Instruction.withString("JMP", "L_while_cond_3"),
                Instruction.withString("LABEL", "L_while_cody_2"),
                Instruction.withString("LD", "x"),
                Instruction.simple("WRITE"),
                Instruction.withString("LD", "x"),
                Instruction.withNumber("CONST", 1),
                Instruction.withString("BINOP", "+"),
                Instruction.withString("ST", "x"),
                Instruction.withString("LABEL", "L_while_cond_3"),
                Instruction.withString("LD", "x"),
                Instruction.withNumber("CONST", 3),
                Instruction.withString("BINOP", "<"),
                Instruction.withString("JNZ", "L_while_cody_2")
        ), result.instructions());

        assertEquals("""
                [
                  { "CONST": 0 },
                  { "ST": "x" },
                  { "JMP": "L_while_cond_3" },
                  { "LABEL": "L_while_cody_2" },
                  { "LD": "x" },
                  "WRITE",
                  { "LD": "x" },
                  { "CONST": 1 },
                  { "BINOP": "+" },
                  { "ST": "x" },
                  { "LABEL": "L_while_cond_3" },
                  { "LD": "x" },
                  { "CONST": 3 },
                  { "BINOP": "<" },
                  { "JNZ": "L_while_cody_2" }
                ]""", result.json());
        assertEquals(List.of(0L, 1L, 2L), result.output());
    }

    @Test
    void compilesIfElseExactlyLikeTheReferenceSite() {
        EngineResult result = LanguageEngine.compileAndRun(
                "if (1) { write(10); } else { write(20); }",
                List.of()
        );

        assertEquals(List.of(
                Instruction.withNumber("CONST", 1),
                Instruction.withString("JZ", "L_else_1"),
                Instruction.withNumber("CONST", 10),
                Instruction.simple("WRITE"),
                Instruction.withString("JMP", "L_end_0"),
                Instruction.withString("LABEL", "L_else_1"),
                Instruction.withNumber("CONST", 20),
                Instruction.simple("WRITE"),
                Instruction.withString("LABEL", "L_end_0")
        ), result.instructions());
        assertEquals(List.of(10L), result.output());
    }

    @Test
    void supportsElifAndChoosesOnlyOneBranch() {
        String source = """
                if (0) {
                    write(1);
                } elif (1) {
                    write(2);
                } else {
                    write(3);
                }
                """;

        EngineResult result = LanguageEngine.compileAndRun(source, List.of());

        assertEquals(List.of(2L), result.output());
        assertEquals(List.of(
                Instruction.withNumber("CONST", 0),
                Instruction.withString("JZ", "L_else_1"),
                Instruction.withNumber("CONST", 1),
                Instruction.simple("WRITE"),
                Instruction.withString("JMP", "L_end_0"),
                Instruction.withString("LABEL", "L_else_1"),
                Instruction.withNumber("CONST", 1),
                Instruction.withString("JZ", "L_else_2"),
                Instruction.withNumber("CONST", 2),
                Instruction.simple("WRITE"),
                Instruction.withString("JMP", "L_end_0"),
                Instruction.withString("LABEL", "L_else_2"),
                Instruction.withNumber("CONST", 3),
                Instruction.simple("WRITE"),
                Instruction.withString("LABEL", "L_end_0")
        ), result.instructions());
    }

    @Test
    void supportsAllStatementsAndOperators() {
        String source = """
                {
                    read(x);
                    read(y);
                    value = x + y * 2;
                    write(value);
                    write(value == 50);
                    write(1 && 7);
                    write(0 !! 7);
                    value -= 2;
                    value *= 2;
                    value /= 4;
                    value %= 10;
                    write(value);
                    skip;
                }
                """;

        EngineResult result = LanguageEngine.compileAndRun(source, List.of(10L, 20L));

        assertEquals(List.of(50L, 1L, 1L, 1L, 4L), result.output());
    }

    @Test
    void usesTheSameOperatorPrecedenceAndAssociativity() {
        String source = """
                {
                    write(2 + 3 * 4);
                    write(10 - 2 - 3);
                    write((1 + 2) * 3);
                    write(100 / 10 / 2);
                    write(1 !! 0 && 0);
                    write((1 !! 0) && 0);
                    write(-5);
                    write(5-2);
                }
                """;

        assertEquals(
                List.of(14L, 5L, 9L, 5L, 1L, 0L, -5L, 3L),
                LanguageEngine.compileAndRun(source, List.of()).output()
        );
    }

    @Test
    void supportsWhileDoWhileAndNestedFor() {
        String source = """
                {
                    i = 5;
                    while (i < 3) write(i);
                    do {
                        write(i);
                        i += 1;
                    } while (i < 3);
                    for (i = 0; i < 2; i += 1) {
                        for (j = 0; j < 2; j += 1) {
                            write(i * 10 + j);
                        }
                    }
                }
                """;

        assertEquals(
                List.of(5L, 0L, 1L, 10L, 11L),
                LanguageEngine.compileAndRun(source, List.of()).output()
        );
    }

    @Test
    void acceptsOptionalSemicolonsAndBothCommentForms() {
        String source = """
                { -- line comment
                    x = 1
                    (* block
                       comment *)
                    x += 1
                    write(x)
                }
                """;

        assertEquals(List.of(2L), LanguageEngine.compileAndRun(source, List.of()).output());
    }

    @Test
    void acceptsAStatementWithoutAnOuterBlock() {
        assertEquals(
                List.of(42L),
                LanguageEngine.compileAndRun("write(42);", List.of()).output()
        );
    }

    @Test
    void parsesCommaOrWhitespaceSeparatedInput() {
        assertEquals(List.of(20L, 22L, -3L), InputValues.parse("20, 22\n-3"));
    }

    @Test
    void rejectsSyntaxThatTheReferenceSiteRejects() {
        assertThrows(LanguageException.class, () -> SourceCompiler.compile("{}"));
        assertThrows(LanguageException.class, () -> SourceCompiler.compile("{ X = 1; }"));
        assertThrows(LanguageException.class, () -> SourceCompiler.compile("{ x' = 1; }"));
        assertThrows(LanguageException.class, () -> SourceCompiler.compile("{ write(1 +); }"));
        assertThrows(LanguageException.class, () -> SourceCompiler.compile("{ (* unterminated }"));
    }

    @Test
    void reportsRuntimeErrors() {
        LanguageException undefined = assertThrows(
                LanguageException.class,
                () -> LanguageEngine.compileAndRun("write(x);", List.of())
        );
        assertTrue(undefined.getMessage().contains("undefined variable"));

        assertThrows(
                LanguageException.class,
                () -> LanguageEngine.compileAndRun("read(x);", List.of())
        );
        assertThrows(
                LanguageException.class,
                () -> LanguageEngine.compileAndRun("write(1 / 0);", List.of())
        );
    }

    @Test
    void matchesReferenceForSkipAndIfWithoutElse() {
        EngineResult skip = LanguageEngine.compileAndRun("skip;", List.of());
        assertEquals(List.of(), skip.instructions());
        assertEquals("[]", skip.json());
        assertEquals(List.of(), skip.output());

        EngineResult conditional = LanguageEngine.compileAndRun("if (0) write(7);", List.of());
        assertEquals(List.of(
                Instruction.withNumber("CONST", 0),
                Instruction.withString("JZ", "L_else_1"),
                Instruction.withNumber("CONST", 7),
                Instruction.simple("WRITE"),
                Instruction.withString("JMP", "L_end_0"),
                Instruction.withString("LABEL", "L_else_1"),
                Instruction.withString("LABEL", "L_end_0")
        ), conditional.instructions());
        assertEquals(List.of(), conditional.output());
    }

    @Test
    void allocatesNestedControlFlowLabelsLikeTheReferenceSite() {
        String source = "{ x = 1; while (x) if (x) x = 0; write(x); }";
        EngineResult result = LanguageEngine.compileAndRun(source, List.of());

        assertEquals(List.of(
                Instruction.withNumber("CONST", 1),
                Instruction.withString("ST", "x"),
                Instruction.withString("JMP", "L_while_cond_4"),
                Instruction.withString("LABEL", "L_while_cody_3"),
                Instruction.withString("LD", "x"),
                Instruction.withString("JZ", "L_else_5"),
                Instruction.withNumber("CONST", 0),
                Instruction.withString("ST", "x"),
                Instruction.withString("JMP", "L_while_cond_4"),
                Instruction.withString("LABEL", "L_else_5"),
                Instruction.withString("LABEL", "L_while_cond_4"),
                Instruction.withString("LD", "x"),
                Instruction.withString("JNZ", "L_while_cody_3"),
                Instruction.withString("LD", "x"),
                Instruction.simple("WRITE")
        ), result.instructions());
        assertEquals(List.of(0L), result.output());
    }
}

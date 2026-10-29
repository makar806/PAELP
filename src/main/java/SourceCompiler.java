import java.util.ArrayList;
import java.util.List;

public final class SourceCompiler {
    private final List<Instruction> code = new ArrayList<>();
    private int labelCounter;

    private SourceCompiler() {
    }

    public static List<Instruction> compile(String source) {
        return compile(Parser.parse(source));
    }

    static List<Instruction> compile(Statement program) {
        SourceCompiler compiler = new SourceCompiler();
        String end = compiler.newLabel("end");
        if (compiler.compileStatement(program, end)) {
            compiler.emitString("LABEL", end);
        }
        return List.copyOf(compiler.code);
    }

    private void compileExpression(Expression expression) {
        if (expression instanceof VariableExpression variable) {
            emitString("LD", variable.name());
            return;
        }
        if (expression instanceof ConstantExpression constant) {
            emitNumber("CONST", constant.value());
            return;
        }
        if (expression instanceof BinaryExpression binary) {
            compileExpression(binary.left());
            compileExpression(binary.right());
            emitString("BINOP", binary.operator());
            return;
        }
        if (expression instanceof StringExpression string) {
            emitString("STRING", string.value());
            return;
        }
        if (expression instanceof ArrayExpression array) {
            array.elements().forEach(this::compileExpression);
            emitNumber("ARRAY", array.elements().size());
            return;
        }
        if (expression instanceof SexpExpression sexp) {
            sexp.arguments().forEach(this::compileExpression);
            code.add(Instruction.sexp(sexp.name(), sexp.arguments().size()));
            return;
        }
        if (expression instanceof ElementExpression element) {
            compileExpression(element.target());
            compileExpression(element.index());
            emitSimple("ELEM");
            return;
        }
        if (expression instanceof LengthExpression length) {
            compileExpression(length.target());
            emitSimple("LENGTH");
            return;
        }
        throw new IllegalStateException("unknown expression: " + expression);
    }

    private boolean compileStatement(Statement statement, String next) {
        if (statement instanceof ReadStatement read) {
            emitSimple("READ");
            emitString("ST", read.variable());
            return false;
        }
        if (statement instanceof WriteStatement write) {
            compileExpression(write.expression());
            emitSimple("WRITE");
            return false;
        }
        if (statement instanceof AssignmentStatement assignment) {
            compileExpression(assignment.expression());
            emitString("ST", assignment.variable());
            return false;
        }
        if (statement instanceof SkipStatement) {
            return false;
        }
        if (statement instanceof SetStatement set) {
            compileExpression(set.target());
            compileExpression(set.index());
            compileExpression(set.value());
            emitSimple("SET");
            return false;
        }
        if (statement instanceof SequenceStatement sequence) {
            String middle = newLabel("seq");
            if (compileStatement(sequence.first(), middle)) {
                emitString("LABEL", middle);
            }
            return compileStatement(sequence.second(), next);
        }
        if (statement instanceof WhileStatement loop) {
            String body = newLabel("while_cody");
            String condition = newLabel("while_cond");
            emitString("JMP", condition);
            emitString("LABEL", body);
            compileStatement(loop.body(), condition);
            emitString("LABEL", condition);
            compileExpression(loop.condition());
            emitString("JNZ", body);
            return false;
        }
        if (statement instanceof DoWhileStatement loop) {
            String body = newLabel("while_cody");
            String condition = newLabel("while_cond");
            emitString("LABEL", body);
            compileStatement(loop.body(), condition);
            emitString("LABEL", condition);
            compileExpression(loop.condition());
            emitString("JNZ", body);
            return false;
        }
        if (statement instanceof IfStatement conditional) {
            String elseLabel = newLabel("else");
            compileExpression(conditional.condition());
            emitString("JZ", elseLabel);
            compileStatement(conditional.thenBranch(), next);
            emitString("JMP", next);
            emitString("LABEL", elseLabel);
            compileStatement(conditional.elseBranch(), next);
            return true;
        }
        throw new IllegalStateException("unknown statement: " + statement);
    }

    private String newLabel(String purpose) {
        return "L_" + purpose + "_" + labelCounter++;
    }

    private void emitSimple(String opcode) {
        code.add(Instruction.simple(opcode));
    }

    private void emitString(String opcode, String argument) {
        code.add(Instruction.withString(opcode, argument));
    }

    private void emitNumber(String opcode, long argument) {
        code.add(Instruction.withNumber(opcode, argument));
    }
}

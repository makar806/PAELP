import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SourceInterpreter {
    private final Map<String, Object> state = new HashMap<>();
    private final Deque<Object> input;
    private final List<Object> output = new ArrayList<>();

    private SourceInterpreter(List<?> input) {
        this.input = new ArrayDeque<>(input);
    }

    public static List<Object> run(String source, List<?> input) {
        return run(Parser.parse(source), input);
    }

    static List<Object> run(Statement statement, List<?> input) {
        SourceInterpreter interpreter = new SourceInterpreter(input);
        interpreter.execute(statement);
        return List.copyOf(interpreter.output);
    }

    private Object evaluate(Expression expression) {
        if (expression instanceof ConstantExpression constant) {
            return constant.value();
        }
        if (expression instanceof VariableExpression variable) {
            return Values.variable(state, variable.name());
        }
        if (expression instanceof StringExpression string) {
            return new Values.Text(string.value());
        }
        if (expression instanceof BinaryExpression binary) {
            Object right = evaluate(binary.right());
            return Values.apply(binary.operator(), evaluate(binary.left()), right);
        }
        if (expression instanceof ElementExpression element) {
            Object index = evaluate(element.index());
            return Values.element(evaluate(element.target()), index);
        }
        if (expression instanceof LengthExpression length) {
            return Values.length(evaluate(length.target()));
        }
        if (expression instanceof ArrayExpression array) {
            return new Values.Array(evaluateAll(array.elements()));
        }
        SexpExpression sexp = (SexpExpression) expression;
        return new Values.Sexp(sexp.name(), evaluateAll(sexp.arguments()));
    }

    private List<Object> evaluateAll(List<Expression> expressions) {
        List<Object> result = new ArrayList<>();
        for (Expression expression : expressions) {
            result.add(evaluate(expression));
        }
        return result;
    }

    private void execute(Statement statement) {
        Deque<Statement> pending = new ArrayDeque<>();
        pending.push(statement);
        while (!pending.isEmpty()) {
            Statement current = pending.pop();
            if (current instanceof ReadStatement read) {
                if (input.isEmpty()) {
                    throw new LanguageException("ERROR: input exhausted");
                }
                state.put(read.variable(), input.removeFirst());
            } else if (current instanceof WriteStatement write) {
                output.add(evaluate(write.expression()));
            } else if (current instanceof AssignmentStatement assignment) {
                state.put(assignment.variable(), evaluate(assignment.expression()));
            } else if (current instanceof SetStatement set) {
                Object value = evaluate(set.value());
                Object index = evaluate(set.index());
                Values.set(evaluate(set.target()), index, value);
            } else if (current instanceof SequenceStatement sequence) {
                pending.push(sequence.second());
                pending.push(sequence.first());
            } else if (current instanceof IfStatement conditional) {
                pending.push(Values.integer(evaluate(conditional.condition())) == 0
                        ? conditional.elseBranch() : conditional.thenBranch());
            } else if (current instanceof WhileStatement loop) {
                if (Values.integer(evaluate(loop.condition())) != 0) {
                    pending.push(current);
                    pending.push(loop.body());
                }
            } else if (current instanceof DoWhileStatement loop) {
                pending.push(new WhileStatement(loop.condition(), loop.body()));
                pending.push(loop.body());
            }
        }
    }
}

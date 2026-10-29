import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class Parser {
    private static final Map<String, Integer> PRECEDENCE = Map.ofEntries(
            Map.entry("!!", 1),
            Map.entry("&&", 2),
            Map.entry("==", 3),
            Map.entry("!=", 3),
            Map.entry("<", 3),
            Map.entry("<=", 3),
            Map.entry(">", 3),
            Map.entry(">=", 3),
            Map.entry(":", 4),
            Map.entry("+", 5),
            Map.entry("-", 5),
            Map.entry("*", 6),
            Map.entry("/", 6),
            Map.entry("%", 6)
    );

    private final List<Token> tokens;
    private int position;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    static Statement parse(String source) {
        Parser parser = new Parser(Lexer.lex(source));
        Statement program = parser.parseStatement();
        parser.expect(TokenKind.EOF, null);
        return program;
    }

    static List<Object> parseInput(String source) {
        Parser parser = new Parser(Lexer.lex(source));
        List<Object> input = new ArrayList<>();
        if (parser.is(TokenKind.EOF, null)) {
            throw fail(parser.peek(), "\"{\" expected");
        }
        do {
            input.add(parser.parseValue());
        } while (parser.accept(TokenKind.COMMA, null));
        parser.expect(TokenKind.EOF, null);
        return List.copyOf(input);
    }

    private Object parseValue() {
        Token token = peek();
        if (accept(TokenKind.LEFT_BRACKET, null)) {
            List<Object> values = parseValues(TokenKind.RIGHT_BRACKET, true);
            return new Values.Array(values);
        }
        if (accept(TokenKind.LEFT_BRACE, null)) {
            return Values.list(parseValues(TokenKind.RIGHT_BRACE, true));
        }
        if (token.kind() == TokenKind.CONSTRUCTOR) {
            next();
            List<Object> arguments = accept(TokenKind.LEFT_PAREN, null)
                    ? parseValues(TokenKind.RIGHT_PAREN, false) : List.of();
            return new Values.Sexp(token.text(), arguments);
        }
        if (token.kind() == TokenKind.STRING) {
            next();
            return new Values.Text(token.text());
        }
        if (token.kind() == TokenKind.INTEGER || token.kind() == TokenKind.OPERATOR
                && token.text().equals("-") && adjacentNumber()) {
            return ((ConstantExpression) parsePrimary()).value();
        }
        throw fail(token, "\"{\" expected");
    }

    private List<Object> parseValues(TokenKind closing, boolean emptyAllowed) {
        List<Object> values = new ArrayList<>();
        if (!emptyAllowed || isValueStart()) {
            do {
                values.add(parseValue());
            } while (accept(TokenKind.COMMA, null));
        }
        expect(closing, null);
        return values;
    }

    private boolean isValueStart() {
        return switch (peek().kind()) {
            case INTEGER, STRING, CONSTRUCTOR, LEFT_BRACE, LEFT_BRACKET -> true;
            case OPERATOR -> peek().text().equals("-") && adjacentNumber();
            default -> false;
        };
    }

    private boolean adjacentNumber() {
        Token token = peek();
        Token number = tokens.get(Math.min(position + 1, tokens.size() - 1));
        return number.kind() == TokenKind.INTEGER && number.line() == token.line()
                && number.column() == token.column() + 1;
    }

    private Statement parseStatement() {
        Token token = peek();
        if (token.kind() == TokenKind.LEFT_BRACE) {
            int start = position;
            try {
                return parseBlock();
            } catch (LanguageException blockError) {
                int blockPosition = position;
                position = start;
                try {
                    return parseAssignment();
                } catch (LanguageException assignmentError) {
                    if (position <= blockPosition) {
                        position = blockPosition;
                        throw blockError;
                    }
                    throw assignmentError;
                }
            }
        }
        if (token.kind() == TokenKind.IDENTIFIER || token.kind() == TokenKind.CONSTRUCTOR
                || token.kind() == TokenKind.LEFT_BRACKET || token.kind() == TokenKind.LEFT_PAREN
                || token.kind() == TokenKind.STRING || token.kind() == TokenKind.INTEGER) {
            return parseAssignment();
        }
        if (token.kind() == TokenKind.KEYWORD) {
            Token following = tokens.get(Math.min(position + 1, tokens.size() - 1));
            if (!token.text().equals("skip") && (following.kind() == TokenKind.ASSIGN
                    || following.kind() == TokenKind.OPERATOR || following.kind() == TokenKind.LEFT_BRACKET)) {
                return parseAssignment();
            }
            return switch (token.text()) {
                case "read" -> parseRead();
                case "write" -> parseWrite();
                case "while" -> parseWhile();
                case "do" -> parseDoWhile();
                case "for" -> parseFor();
                case "if" -> parseIf();
                case "skip" -> parseSkip();
                default -> throw fail(token, "expected statement, got " + token.description());
            };
        }
        throw fail(token, "\"{\" expected");
    }

    private Statement parseBlock() {
        expect(TokenKind.LEFT_BRACE, null);
        if (is(TokenKind.RIGHT_BRACE, null)) {
            next();
            throw fail(peek(), "\"[\" expected");
        }
        List<Statement> statements = new ArrayList<>();
        do {
            statements.add(parseStatement());
        } while (switch (peek().kind()) {
            case IDENTIFIER, CONSTRUCTOR, LEFT_BRACKET, LEFT_PAREN, LEFT_BRACE, STRING, INTEGER, KEYWORD -> true;
            default -> false;
        });
        expect(TokenKind.RIGHT_BRACE, null);
        return sequence(statements);
    }

    private Statement parseRead() {
        expectKeyword("read");
        expect(TokenKind.LEFT_PAREN, null);
        String variable = expectIdentifier().text();
        expect(TokenKind.RIGHT_PAREN, null);
        accept(TokenKind.SEMICOLON, null);
        return new ReadStatement(variable);
    }

    private Statement parseWrite() {
        expectKeyword("write");
        expect(TokenKind.LEFT_PAREN, null);
        Expression expression = parseExpression();
        expect(TokenKind.RIGHT_PAREN, null);
        accept(TokenKind.SEMICOLON, null);
        return new WriteStatement(expression);
    }

    private Statement parseAssignment() {
        Expression destination = parsePrimary();
        while (accept(TokenKind.LEFT_BRACKET, null)) {
            Expression index = parseExpression();
            expect(TokenKind.RIGHT_BRACKET, null);
            destination = new ElementExpression(destination, index);
        }
        if (!(destination instanceof VariableExpression) && !(destination instanceof ElementExpression)) {
            throw fail(peek(), "\"[\" expected");
        }
        String compoundOperator = null;
        if (is(TokenKind.OPERATOR, null)) {
            compoundOperator = next().text();
        }
        expect(TokenKind.ASSIGN, null);
        Expression expression = parseExpression();
        accept(TokenKind.SEMICOLON, null);
        if (compoundOperator != null) {
            expression = new BinaryExpression(compoundOperator, destination, expression);
        }
        if (destination instanceof VariableExpression variable) {
            return new AssignmentStatement(variable.name(), expression);
        }
        if (destination instanceof ElementExpression element) {
            return new SetStatement(element.target(), element.index(), expression);
        }
        throw fail(peek(), "assignment destination expected");
    }

    private Statement parseWhile() {
        expectKeyword("while");
        expect(TokenKind.LEFT_PAREN, null);
        Expression condition = parseExpression();
        expect(TokenKind.RIGHT_PAREN, null);
        return new WhileStatement(condition, parseStatement());
    }

    private Statement parseDoWhile() {
        expectKeyword("do");
        Statement body = parseStatement();
        expectKeyword("while");
        expect(TokenKind.LEFT_PAREN, null);
        Expression condition = parseExpression();
        expect(TokenKind.RIGHT_PAREN, null);
        accept(TokenKind.SEMICOLON, null);
        return new DoWhileStatement(body, condition);
    }

    private Statement parseFor() {
        expectKeyword("for");
        expect(TokenKind.LEFT_PAREN, null);
        Statement initialization = parseStatement();
        Expression condition = parseExpression();
        expect(TokenKind.SEMICOLON, null);
        Statement step = parseStatement();
        expect(TokenKind.RIGHT_PAREN, null);
        Statement body = parseStatement();

        return new SequenceStatement(
                initialization,
                new WhileStatement(condition, new SequenceStatement(body, step))
        );
    }

    private Statement parseIf() {
        if (!isKeyword("if") && !isKeyword("elif")) {
            throw fail(peek(), "expected if or elif");
        }
        next();
        expect(TokenKind.LEFT_PAREN, null);
        Expression condition = parseExpression();
        expect(TokenKind.RIGHT_PAREN, null);
        Statement thenBranch = parseStatement();
        Statement elseBranch = new SkipStatement();
        if (acceptKeyword("else")) {
            elseBranch = parseStatement();
        } else if (isKeyword("elif")) {
            elseBranch = parseIf();
        }
        return new IfStatement(condition, thenBranch, elseBranch);
    }

    private Statement parseSkip() {
        expectKeyword("skip");
        accept(TokenKind.SEMICOLON, null);
        return new SkipStatement();
    }

    private Expression parseExpression() {
        return parseBinary(1);
    }

    private Expression parseBinary(int minimumPrecedence) {
        Expression left = parsePostfix();
        boolean compared = false;
        while (is(TokenKind.OPERATOR, null)) {
            Integer precedence = PRECEDENCE.get(peek().text());
            if (precedence == null || precedence < minimumPrecedence) {
                break;
            }
            if (precedence == 3 && compared) {
                break;
            }
            String operator = next().text();
            Expression right = parseBinary(operator.equals(":") ? precedence : precedence + 1);
            left = binary(operator, left, right);
            compared |= precedence == 3;
        }
        return left;
    }

    private static Expression binary(String operator, Expression left, Expression right) {
        return operator.equals(":") ? new SexpExpression("cons", List.of(left, right))
                : new BinaryExpression(operator, left, right);
    }

    private Expression parsePostfix() {
        Expression result = parsePrimary();
        while (true) {
            if (accept(TokenKind.LEFT_BRACKET, null)) {
                Expression index = parseExpression();
                expect(TokenKind.RIGHT_BRACKET, null);
                result = new ElementExpression(result, index);
            } else if (accept(TokenKind.DOT, null)) {
                expect(TokenKind.IDENTIFIER, "length");
                result = new LengthExpression(result);
            } else {
                return result;
            }
        }
    }

    private List<Expression> parseExpressions(TokenKind closing, boolean emptyAllowed) {
        List<Expression> expressions = new ArrayList<>();
        if (!emptyAllowed || !is(closing, null)) {
            do {
                expressions.add(parseExpression());
            } while (accept(TokenKind.COMMA, null));
        }
        expect(closing, null);
        return List.copyOf(expressions);
    }

    private Expression parsePrimary() {
        Token token = peek();
        if (token.kind() == TokenKind.IDENTIFIER || token.kind() == TokenKind.KEYWORD) {
            next();
            return new VariableExpression(token.text());
        }
        if (token.kind() == TokenKind.INTEGER) {
            next();
            if (token.number() > Integer.MAX_VALUE) {
                throw new LanguageException("Failure(\"int_of_string\")");
            }
            return new ConstantExpression(token.number());
        }
        if (token.kind() == TokenKind.STRING) {
            next();
            return new StringExpression(token.text());
        }
        if (token.kind() == TokenKind.CONSTRUCTOR) {
            next();
            return new SexpExpression(token.text(), accept(TokenKind.LEFT_PAREN, null)
                    ? parseExpressions(TokenKind.RIGHT_PAREN, false) : List.of());
        }
        if (accept(TokenKind.LEFT_BRACKET, null)) {
            return new ArrayExpression(parseExpressions(TokenKind.RIGHT_BRACKET, false));
        }
        if (accept(TokenKind.LEFT_BRACE, null)) {
            List<Expression> elements = parseExpressions(TokenKind.RIGHT_BRACE, true);
            Expression list = new SexpExpression("nil", List.of());
            for (int i = elements.size() - 1; i >= 0; i--) {
                list = new SexpExpression("cons", List.of(elements.get(i), list));
            }
            return list;
        }
        if (token.kind() == TokenKind.OPERATOR && token.text().equals("-") && adjacentNumber()) {
            next();
            Token number = expect(TokenKind.INTEGER, null);
            return new ConstantExpression(-number.number());
        }
        if (accept(TokenKind.LEFT_PAREN, null)) {
            Expression expression = parseExpression();
            expect(TokenKind.RIGHT_PAREN, null);
            return expression;
        }
        throw fail(token, "\"(\" expected");
    }

    private static Statement sequence(List<Statement> statements) {
        Statement result = statements.get(statements.size() - 1);
        for (int i = statements.size() - 2; i >= 0; i--) {
            result = new SequenceStatement(statements.get(i), result);
        }
        return result;
    }

    private Token peek() {
        return tokens.get(position);
    }

    private Token next() {
        Token token = peek();
        if (token.kind() != TokenKind.EOF) {
            position++;
        }
        return token;
    }

    private boolean is(TokenKind kind, String text) {
        Token token = peek();
        return token.kind() == kind && (text == null || token.text().equals(text));
    }

    private boolean isKeyword(String keyword) {
        return is(TokenKind.KEYWORD, keyword);
    }

    private boolean accept(TokenKind kind, String text) {
        if (!is(kind, text)) {
            return false;
        }
        next();
        return true;
    }

    private boolean acceptKeyword(String keyword) {
        return accept(TokenKind.KEYWORD, keyword);
    }

    private Token expectKeyword(String keyword) {
        return expect(TokenKind.KEYWORD, keyword);
    }

    private Token expectIdentifier() {
        if (peek().kind() == TokenKind.IDENTIFIER || peek().kind() == TokenKind.KEYWORD) {
            return next();
        }
        throw fail(peek(), "\"l-identifier\" expected");
    }

    private Token expect(TokenKind kind, String text) {
        if (!is(kind, text)) {
            String expected = text != null ? '"' + text + '"' : kindName(kind);
            throw fail(peek(), expected + " expected");
        }
        return next();
    }

    private static String kindName(TokenKind kind) {
        return switch (kind) {
            case EOF -> "<EOF>";
            case UNKNOWN -> "token";
            case IDENTIFIER -> "identifier";
            case CONSTRUCTOR -> "constructor";
            case STRING -> "string";
            case INTEGER -> "integer";
            case KEYWORD -> "keyword";
            case OPERATOR -> "operator";
            case ASSIGN -> "\"=\"";
            case LEFT_PAREN -> "\"(\"";
            case RIGHT_PAREN -> "\")\"";
            case LEFT_BRACE -> "\"{\"";
            case RIGHT_BRACE -> "\"}\"";
            case LEFT_BRACKET -> "\"[\"";
            case RIGHT_BRACKET -> "\"]\"";
            case COMMA -> "\",\"";
            case DOT -> "\".\"";
            case SEMICOLON -> "\";\"";
        };
    }

    private static LanguageException fail(Token token, String message) {
        return new LanguageException(message + " at (" + token.line() + ":" + token.column() + ")");
    }
}

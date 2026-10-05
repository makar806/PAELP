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
            Map.entry("+", 4),
            Map.entry("-", 4),
            Map.entry("*", 5),
            Map.entry("/", 5),
            Map.entry("%", 5)
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

    private Statement parseStatement() {
        Token token = peek();
        if (token.kind() == TokenKind.LEFT_BRACE) {
            return parseBlock();
        }
        if (token.kind() == TokenKind.IDENTIFIER) {
            return parseAssignment();
        }
        if (token.kind() == TokenKind.KEYWORD) {
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
        throw fail(token, "expected statement, got " + token.description());
    }

    private Statement parseBlock() {
        expect(TokenKind.LEFT_BRACE, null);
        if (is(TokenKind.RIGHT_BRACE, null)) {
            throw fail(peek(), "empty blocks are not allowed");
        }
        List<Statement> statements = new ArrayList<>();
        do {
            statements.add(parseStatement());
        } while (!is(TokenKind.RIGHT_BRACE, null));
        expect(TokenKind.RIGHT_BRACE, null);
        return sequence(statements);
    }

    private Statement parseRead() {
        expectKeyword("read");
        expect(TokenKind.LEFT_PAREN, null);
        String variable = expect(TokenKind.IDENTIFIER, null).text();
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
        String variable = expect(TokenKind.IDENTIFIER, null).text();
        String compoundOperator = null;
        if (is(TokenKind.OPERATOR, null)) {
            compoundOperator = next().text();
        }
        expect(TokenKind.ASSIGN, null);
        Expression expression = parseExpression();
        accept(TokenKind.SEMICOLON, null);
        if (compoundOperator != null) {
            expression = new BinaryExpression(
                    compoundOperator,
                    new VariableExpression(variable),
                    expression
            );
        }
        return new AssignmentStatement(variable, expression);
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
        Expression left = parsePrimary();
        while (is(TokenKind.OPERATOR, null)) {
            Integer precedence = PRECEDENCE.get(peek().text());
            if (precedence == null || precedence < minimumPrecedence) {
                break;
            }
            String operator = next().text();
            Expression right = parseBinary(precedence + 1);
            left = new BinaryExpression(operator, left, right);
        }
        return left;
    }

    private Expression parsePrimary() {
        Token token = peek();
        if (token.kind() == TokenKind.IDENTIFIER) {
            next();
            return new VariableExpression(token.text());
        }
        if (token.kind() == TokenKind.INTEGER) {
            next();
            return new ConstantExpression(token.number());
        }
        if (token.kind() == TokenKind.OPERATOR && token.text().equals("-")) {
            next();
            Token number = expect(TokenKind.INTEGER, null);
            return new ConstantExpression(-number.number());
        }
        if (accept(TokenKind.LEFT_PAREN, null)) {
            Expression expression = parseExpression();
            expect(TokenKind.RIGHT_PAREN, null);
            return expression;
        }
        throw fail(token, "expected expression, got " + token.description());
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

    private Token expect(TokenKind kind, String text) {
        if (!is(kind, text)) {
            String expected = text != null ? '"' + text + '"' : kindName(kind);
            throw fail(peek(), "expected " + expected + ", got " + peek().description());
        }
        return next();
    }

    private static String kindName(TokenKind kind) {
        return switch (kind) {
            case EOF -> "end of file";
            case IDENTIFIER -> "identifier";
            case INTEGER -> "integer";
            case KEYWORD -> "keyword";
            case OPERATOR -> "operator";
            case ASSIGN -> "\"=\"";
            case LEFT_PAREN -> "\"(\"";
            case RIGHT_PAREN -> "\")\"";
            case LEFT_BRACE -> "\"{\"";
            case RIGHT_BRACE -> "\"}\"";
            case SEMICOLON -> "\";\"";
        };
    }

    private static LanguageException fail(Token token, String message) {
        return new LanguageException(token.line() + ":" + token.column() + ": " + message);
    }
}

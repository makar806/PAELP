import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class Lexer {
    private static final Set<String> KEYWORDS = Set.of(
            "read", "write", "while", "do", "for", "if", "elif", "else", "skip"
    );

    private static final List<String> OPERATORS = List.of(
            "!!", "&&", "==", "!=", "<=", ">=", "<", ">", "+", "-", "*", "/", "%"
    );

    private final String source;
    private int position;
    private int line = 1;
    private int column = 1;

    private Lexer(String source) {
        this.source = source;
    }

    static List<Token> lex(String source) {
        return new Lexer(source).lexAll();
    }

    private List<Token> lexAll() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            Token token = nextToken();
            tokens.add(token);
            if (token.kind() == TokenKind.EOF) {
                return List.copyOf(tokens);
            }
        }
    }

    private Token nextToken() {
        skipWhitespaceAndComments();
        int tokenLine = line;
        int tokenColumn = column;

        if (atEnd()) {
            return Token.simple(TokenKind.EOF, "", tokenLine, tokenColumn);
        }

        char current = peek(0);
        if (isLowercaseLetter(current)) {
            int start = position;
            while (isIdentifierCharacter(peek(0))) {
                advance();
            }
            String text = source.substring(start, position);
            return Token.simple(KEYWORDS.contains(text) ? TokenKind.KEYWORD : TokenKind.IDENTIFIER,
                    text, tokenLine, tokenColumn);
        }

        if (isDigit(current)) {
            int start = position;
            while (isDigit(peek(0))) {
                advance();
            }
            String text = source.substring(start, position);
            try {
                return Token.integer(text, Long.parseLong(text), tokenLine, tokenColumn);
            } catch (NumberFormatException exception) {
                throw error(tokenLine, tokenColumn, "integer constant is out of range: " + text);
            }
        }

        for (String operator : OPERATORS) {
            if (hasPrefix(operator)) {
                advance(operator.length());
                return Token.simple(TokenKind.OPERATOR, operator, tokenLine, tokenColumn);
            }
        }

        TokenKind kind = switch (current) {
            case '=' -> TokenKind.ASSIGN;
            case '(' -> TokenKind.LEFT_PAREN;
            case ')' -> TokenKind.RIGHT_PAREN;
            case '{' -> TokenKind.LEFT_BRACE;
            case '}' -> TokenKind.RIGHT_BRACE;
            case ';' -> TokenKind.SEMICOLON;
            default -> null;
        };
        if (kind != null) {
            advance();
            return Token.simple(kind, Character.toString(current), tokenLine, tokenColumn);
        }

        throw error(tokenLine, tokenColumn, "unexpected character: " + quoted(current));
    }

    private void skipWhitespaceAndComments() {
        while (!atEnd()) {
            if (Character.isWhitespace(peek(0))) {
                advance();
                continue;
            }
            if (hasPrefix("--")) {
                while (!atEnd() && peek(0) != '\n') {
                    advance();
                }
                continue;
            }
            if (hasPrefix("(*")) {
                int commentLine = line;
                int commentColumn = column;
                advance(2);
                while (!hasPrefix("*)")) {
                    if (atEnd()) {
                        throw error(commentLine, commentColumn, "unterminated comment");
                    }
                    advance();
                }
                advance(2);
                continue;
            }
            return;
        }
    }

    private boolean hasPrefix(String value) {
        return source.startsWith(value, position);
    }

    private char peek(int offset) {
        int index = position + offset;
        return index < source.length() ? source.charAt(index) : '\0';
    }

    private void advance(int count) {
        for (int i = 0; i < count; i++) {
            advance();
        }
    }

    private void advance() {
        char current = source.charAt(position++);
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    private boolean atEnd() {
        return position >= source.length();
    }

    private static boolean isLowercaseLetter(char value) {
        return value >= 'a' && value <= 'z';
    }

    private static boolean isIdentifierCharacter(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || isDigit(value)
                || value == '_';
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static String quoted(char value) {
        return value == '\0' ? "end of file" : "\"" + value + "\"";
    }

    private static LanguageException error(int line, int column, String message) {
        return new LanguageException(line + ":" + column + ": " + message);
    }
}

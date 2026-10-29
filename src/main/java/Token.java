enum TokenKind {
    EOF,
    UNKNOWN,
    IDENTIFIER,
    CONSTRUCTOR,
    STRING,
    INTEGER,
    KEYWORD,
    OPERATOR,
    ASSIGN,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    COMMA,
    DOT,
    SEMICOLON
}

record Token(TokenKind kind, String text, long number, int line, int column) {
    static Token simple(TokenKind kind, String text, int line, int column) {
        return new Token(kind, text, 0, line, column);
    }

    static Token integer(String text, long number, int line, int column) {
        return new Token(TokenKind.INTEGER, text, number, line, column);
    }

    String description() {
        return kind == TokenKind.EOF ? "end of file" : '"' + text + '"';
    }
}

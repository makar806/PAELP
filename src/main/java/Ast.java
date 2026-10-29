sealed interface Expression permits VariableExpression, ConstantExpression, BinaryExpression,
        StringExpression, ArrayExpression, SexpExpression, ElementExpression, LengthExpression {
}

record VariableExpression(String name) implements Expression {
}

record ConstantExpression(long value) implements Expression {
}

record BinaryExpression(String operator, Expression left, Expression right) implements Expression {
}

record StringExpression(String value) implements Expression {
}

record ArrayExpression(java.util.List<Expression> elements) implements Expression {
}

record SexpExpression(String name, java.util.List<Expression> arguments) implements Expression {
}

record ElementExpression(Expression target, Expression index) implements Expression {
}

record LengthExpression(Expression target) implements Expression {
}

sealed interface Statement permits ReadStatement, WriteStatement, AssignmentStatement,
        SequenceStatement, WhileStatement, DoWhileStatement, IfStatement, SkipStatement, SetStatement {
}

record ReadStatement(String variable) implements Statement {
}

record WriteStatement(Expression expression) implements Statement {
}

record AssignmentStatement(String variable, Expression expression) implements Statement {
}

record SequenceStatement(Statement first, Statement second) implements Statement {
}

record WhileStatement(Expression condition, Statement body) implements Statement {
}

record DoWhileStatement(Statement body, Expression condition) implements Statement {
}

record IfStatement(Expression condition, Statement thenBranch, Statement elseBranch) implements Statement {
}

record SkipStatement() implements Statement {
}

record SetStatement(Expression target, Expression index, Expression value) implements Statement {
}
